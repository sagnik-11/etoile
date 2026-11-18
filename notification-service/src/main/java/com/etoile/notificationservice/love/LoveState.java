package com.etoile.notificationservice.love;

import com.etoile.notificationservice.security.Role;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "love_state")
public class LoveState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // The person this state belongs to (the one receiving love / being asked for love).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 10)
    private Role recipient;

    @Column(name = "love_count", nullable = false)
    private int loveCount;            // love received since last seen

    @Column(name = "love_pushed", nullable = false)
    private boolean lovePushed;       // a buzzing love push already went out since last seen

    @Column(name = "need_since")
    private Instant needSince;        // the other person needs love since this time (null = not needed)

    @Column(name = "need_presses", nullable = false)
    private int needPresses;          // how many times "Need love" was pressed this round

    protected LoveState() {
    }

    public LoveState(Role recipient) {
        this.recipient = recipient;
    }

    public int addLove(int taps) {
        loveCount += taps;
        return loveCount;
    }

    // Returns true only the first time since last seen (that push should buzz).
    public boolean markLovePushed() {
        boolean first = !lovePushed;
        lovePushed = true;
        return first;
    }

    public int pressNeed() {
        needPresses++;
        needSince = Instant.now();      // restarts the 30-minute window
        return needPresses;
    }

    public void seen() {
        loveCount = 0;
        lovePushed = false;
        needSince = null;
        needPresses = 0;
    }

    public int getLoveCount() { return loveCount; }
    public Instant getNeedSince() { return needSince; }
}
