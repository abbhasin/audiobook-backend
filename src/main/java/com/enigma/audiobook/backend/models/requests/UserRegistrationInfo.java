package com.enigma.audiobook.backend.models.requests;

import lombok.Data;

@Data
public class UserRegistrationInfo {
    String userId;
    String authUserId;
    String phoneNumber;
}
