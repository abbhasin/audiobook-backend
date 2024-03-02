package com.enigma.audiobook.backend.proxies;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.*;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class FirebaseClient {
    private final FirebaseApp app;
    private final FirebaseAuth defaultAuth;

    public FirebaseClient() throws IOException {
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setProjectId("enigmaticaudiobook")
                .build();

        app = FirebaseApp.initializeApp(options);
        defaultAuth = FirebaseAuth.getInstance(app);
    }

    public Optional<FirebaseUserInfo> getUserInfo(String uid) {
        try {
            UserRecord userRecord = defaultAuth.getUser(uid);
            if (userRecord == null) {
                return Optional.empty();
            }

            FirebaseUserInfo userInfo = new FirebaseUserInfo();
            userInfo.setUid(uid);
            userInfo.setPhoneNum(userRecord.getPhoneNumber());

            return Optional.of(userInfo);
        } catch (FirebaseAuthException authEx) {
            if (authEx.getAuthErrorCode() != null &&
                    authEx.getAuthErrorCode().equals(AuthErrorCode.USER_NOT_FOUND)) {
                return Optional.empty();
            }
            throw new RuntimeException(authEx);
        }
    }

    public boolean isValidUser(String uid) {
        try {
            UserRecord userRecord = defaultAuth.getUser(uid);
            return userRecord != null;
        } catch (FirebaseAuthException authEx) {
            if (authEx.getAuthErrorCode() != null &&
                    authEx.getAuthErrorCode().equals(AuthErrorCode.USER_NOT_FOUND)) {
                return false;
            }
            throw new RuntimeException(authEx);
        }
    }

    public boolean isValidIdToken(String idToken) {
        try {
            FirebaseToken token = defaultAuth.verifyIdToken(idToken);
            return token != null;
        } catch (FirebaseAuthException e) {
            if (e.getAuthErrorCode() != null &&
                    (e.getAuthErrorCode().equals(AuthErrorCode.REVOKED_ID_TOKEN) ||
                            e.getAuthErrorCode().equals(AuthErrorCode.INVALID_ID_TOKEN)
                    )) {
                return false;
            }
            throw new RuntimeException(e);
        }
    }

    public Optional<FirebaseUserInfo> getUserFromToken(String idToken) {
        try {
            FirebaseToken token = defaultAuth.verifyIdToken(idToken);
            if (token == null) {
                return Optional.empty();
            }

            FirebaseUserInfo userInfo = new FirebaseUserInfo();
            userInfo.setUid(token.getUid());

            return Optional.of(userInfo);
        } catch (FirebaseAuthException e) {
            if (e.getAuthErrorCode() != null &&
                    (e.getAuthErrorCode().equals(AuthErrorCode.REVOKED_ID_TOKEN) ||
                            e.getAuthErrorCode().equals(AuthErrorCode.INVALID_ID_TOKEN)
                    )) {
                return Optional.empty();
            }
            throw new RuntimeException(e);
        }
    }

    @Data
    public static class FirebaseUserInfo {
        String uid;
        String phoneNum;
    }
}
