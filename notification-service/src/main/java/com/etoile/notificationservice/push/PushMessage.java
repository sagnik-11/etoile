package com.etoile.notificationservice.push;

import java.util.Map;

// groupKey: notifications with the same key replace each other on the phone (merging).
// data: extra info for the app, e.g. which date to open when tapped.
public record PushMessage(String groupKey, String title, String body, Map<String, String> data) {
}
