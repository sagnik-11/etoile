package com.etoile.notificationservice.push;

import com.etoile.notificationservice.device.DeviceToken;
import com.etoile.notificationservice.device.DeviceTokenRepository;
import com.etoile.notificationservice.security.Role;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
public class PushSender {

    private static final Logger log = LoggerFactory.getLogger(PushSender.class);

    private final DeviceTokenRepository devices;
    private final FirebaseMessaging messaging;   // null = log-only mode

    public PushSender(DeviceTokenRepository devices,
                      @Value("${firebase.credentials:}") String credentials) throws IOException {
        this.devices = devices;
        this.messaging = credentials.isBlank() ? null : connect(credentials);
        log.info(messaging == null
                ? "FIREBASE_CREDENTIALS not set: LOG-ONLY mode, no real pushes"
                : "Firebase connected: real pushes enabled");
    }

    public boolean isLive() {
        return messaging != null;
    }

    public void send(Role to, PushMessage m) {
        List<DeviceToken> targets = devices.findByRole(to);
        if (messaging == null) {
            log.info("[LOG-ONLY] would send to {} ({} device(s)) [{}] {} | {}",
                    to, targets.size(), m.groupKey(), m.title(), m.body());
            return;
        }
        log.info("Sending to {} ({} device(s)) [{}] {} | {}", to, targets.size(), m.groupKey(), m.title(), m.body());
        for (DeviceToken device : targets) {
            deliver(to, device, Message.builder()
                    .setToken(device.getToken())
                    .setNotification(Notification.builder().setTitle(m.title()).setBody(m.body()).build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder().setTag(m.groupKey()).build())
                            .build())
                    .putAllData(m.data())
                    .build());
        }
    }

    // Data-only push: no sound by itself. The phone app replaces the notification with this tag quietly.
    public void sendSilent(Role to, PushMessage m) {
        List<DeviceToken> targets = devices.findByRole(to);
        if (messaging == null) {
            log.info("[LOG-ONLY] would quietly update {} ({} device(s)) [{}] {} | {}",
                    to, targets.size(), m.groupKey(), m.title(), m.body());
            return;
        }
        log.info("Quietly updating {} ({} device(s)) [{}] {} | {}", to, targets.size(), m.groupKey(), m.title(), m.body());
        for (DeviceToken device : targets) {
            deliver(to, device, Message.builder()
                    .setToken(device.getToken())
                    .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
                    .putAllData(m.data())
                    .putData("silent", "true")
                    .putData("tag", m.groupKey())
                    .putData("title", m.title())
                    .putData("body", m.body())
                    .build());
        }
    }

    private void deliver(Role to, DeviceToken device, Message message) {
        try {
            messaging.send(message);
            log.info("Delivered to {}", to);
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode code = e.getMessagingErrorCode();
            ErrorCode general = e.getErrorCode();   // works even when Firebase's reply couldn't be read
            boolean deadToken = code == MessagingErrorCode.UNREGISTERED
                    || code == MessagingErrorCode.INVALID_ARGUMENT
                    || general == ErrorCode.INVALID_ARGUMENT
                    || general == ErrorCode.NOT_FOUND;
            if (deadToken) {
                log.warn("Removing dead device token for {} ({})", to, code != null ? code : general);
                devices.delete(device);
            } else {
                log.error("Push to {} failed: {}", to, e.getMessage());
            }
        }
    }

    private static FirebaseMessaging connect(String base64Key) throws IOException {
        byte[] json = Base64.getDecoder().decode(base64Key.trim());
        GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(json));
        FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
        FirebaseApp app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
        return FirebaseMessaging.getInstance(app);
    }
}
