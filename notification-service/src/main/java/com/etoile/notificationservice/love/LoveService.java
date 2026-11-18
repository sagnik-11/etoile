package com.etoile.notificationservice.love;

import com.etoile.notificationservice.love.LoveDtos.LoveResponse;
import com.etoile.notificationservice.push.PushMessage;
import com.etoile.notificationservice.push.PushSender;
import com.etoile.notificationservice.reminder.ReminderKind;
import com.etoile.notificationservice.reminder.Reminders;
import com.etoile.notificationservice.security.CurrentUser;
import com.etoile.notificationservice.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class LoveService {

    static final String TAG = "love";   // all love pushes merge into one notification

    private final LoveStateRepository states;
    private final PushSender sender;
    private final Reminders reminders;

    public LoveService(LoveStateRepository states, PushSender sender, Reminders reminders) {
        this.states = states;
        this.sender = sender;
        this.reminders = reminders;
    }

    @Transactional
    public LoveResponse send(CurrentUser me, int taps) {
        Role to = me.role().other();
        LoveState s = stateOf(to);
        int total = s.addLove(taps);
        PushMessage m = new PushMessage(TAG, "Love 💗", loveText(me.name(), total),
                Map.of("type", "LOVE", "count", String.valueOf(total)));
        if (s.markLovePushed()) {
            sender.send(to, m);          // first one since seen: buzz
        } else {
            sender.sendSilent(to, m);    // already buzzed: just update the text quietly
        }
        return view(me.role());
    }

    @Transactional
    public LoveResponse need(CurrentUser me) {
        Role to = me.role().other();
        int presses = stateOf(to).pressNeed();
        sender.send(to, new PushMessage(TAG, "Need love 🥺", needText(me.name(), presses),
                Map.of("type", "NEED_LOVE")));
        reminders.start(ReminderKind.NEED_LOVE, to, TAG, "Need love 🥺",
                "Still waiting… " + me.name() + " really needs that love 🥺", null, null);
        return view(me.role());
    }

    // Tapping the love icon (or the love push): clears count, highlight, block and Need-love reminders.
    @Transactional
    public LoveResponse seen(CurrentUser me) {
        stateOf(me.role()).seen();
        reminders.stop(ReminderKind.NEED_LOVE, me.role(), TAG);
        return view(me.role());
    }

    @Transactional(readOnly = true)
    public LoveResponse view(Role me) {
        LoveState mine = states.findByRecipient(me).orElse(null);
        LoveState theirs = states.findByRecipient(me.other()).orElse(null);
        return new LoveResponse(
                mine == null ? 0 : mine.getLoveCount(),
                mine != null && mine.getNeedSince() != null,
                theirs != null && theirs.getNeedSince() != null);
    }

    static String loveText(String who, int n) {
        if (n == 1) return who + " has sent you love 💗";
        if (n <= 4) return who + " has sent you love 💗 ×" + n;
        if (n <= 9) return "Incoming love overload 💗💗💗 ×" + n + " from " + who;
        if (n <= 24) return "⚠️ Love levels critical! " + who + " sent ×" + n + " 💗";
        return "🚨 System overloaded with love. " + who + " won't stop 💗 ×" + n;
    }

    // Each extra press in the same round gets cheekier.
    static String needText(String who, int presses) {
        return switch (presses) {
            case 1 -> who + " is missing you 🥺 Send some love?";
            case 2 -> who + " is missing you even MORE now 🥺🥺";
            case 3 -> "Second request: " + who + " still needs love. This is not a drill 🚨";
            default -> who + " pressed it again. Things are getting serious 🥺💔";
        };
    }

    private LoveState stateOf(Role recipient) {
        return states.findByRecipient(recipient).orElseGet(() -> states.save(new LoveState(recipient)));
    }
}
