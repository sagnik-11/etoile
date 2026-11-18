package com.etoile.notificationservice.push;

import com.etoile.notificationservice.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationGroups {

    private final NotificationGroupRepository groups;

    public NotificationGroups(NotificationGroupRepository groups) {
        this.groups = groups;
    }

    // One more unseen push in this group; returns the new total (used for "×3" texts).
    @Transactional
    public int bump(Role to, String groupKey) {
        NotificationGroup g = groups.findByRecipientAndGroupKey(to, groupKey)
                .orElseGet(() -> groups.save(new NotificationGroup(to, groupKey)));
        return g.bump();
    }

    @Transactional(readOnly = true)
    public int unseen(Role to, String groupKey) {
        return groups.findByRecipientAndGroupKey(to, groupKey).map(NotificationGroup::getUnseenCount).orElse(0);
    }

    @Transactional
    public void seen(Role me, String groupKey) {
        groups.deleteByRecipientAndGroupKey(me, groupKey);
    }
}
