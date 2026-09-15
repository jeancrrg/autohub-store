package com.autohubstore.userservice.application.usecase;

import java.util.UUID;

public interface UpdatePasswordUseCase {

    void updatePassword(UUID userId, String newPassword);

}
