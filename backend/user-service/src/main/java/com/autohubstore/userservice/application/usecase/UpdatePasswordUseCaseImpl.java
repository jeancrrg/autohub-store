package com.autohubstore.userservice.application.usecase;

import com.autohubstore.userservice.domain.model.User;
import com.autohubstore.userservice.domain.repository.UserRepository;
import com.autohubstore.userservice.domain.service.PasswordDomainService;
import com.autohubstore.userservice.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdatePasswordUseCaseImpl implements UpdatePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordDomainService passwordDomainService;

    @Override
    @Transactional
    public void updatePassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
        user.setPasswordHash(passwordDomainService.hash(newPassword));
        userRepository.save(user);
    }

}
