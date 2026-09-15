package com.autohubstore.userservice.application.usecase;

import com.autohubstore.userservice.application.dto.request.CreateUserRequest;
import com.autohubstore.userservice.application.dto.response.UserResponse;
import com.autohubstore.userservice.application.mapper.UserMapper;
import com.autohubstore.userservice.domain.event.UserCreatedEvent;
import com.autohubstore.userservice.domain.model.User;
import com.autohubstore.userservice.domain.repository.UserEventPublisher;
import com.autohubstore.userservice.domain.repository.UserRepository;
import com.autohubstore.userservice.domain.service.PasswordDomainService;
import com.autohubstore.userservice.exception.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateUserUseCaseImpl implements CreateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordDomainService passwordDomainService;
    private final UserEventPublisher userEventPublisher;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = userMapper.toDomain(request);
        user.setPasswordHash(passwordDomainService.hash(request.password()));
        user = userRepository.save(user);

        userEventPublisher.publishUserCreated(new UserCreatedEvent(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getCreatedAt()
        ));

        return userMapper.toResponse(user);
    }

}
