package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.models.User;
import lombok.Data;

@Data
public class UserAssociationResponse {
    final UserAssociationStatus associationStatus;
    final User user;

    public enum UserAssociationStatus {
        MAPPED_TO_EXISTING_USER,
        MAPPED_TO_GIVEN_USER
    }
}
