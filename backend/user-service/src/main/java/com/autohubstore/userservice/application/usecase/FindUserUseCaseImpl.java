package com.autohubstore.userservice.application.usecase;

import com.autohubstore.userservice.application.dto.response.UserResponse;
import com.autohubstore.userservice.application.mapper.UserMapper;
import com.autohubstore.userservice.domain.repository.UserRepository;
import com.autohubstore.userservice.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FindUserUseCaseImpl implements FindUserUseCase {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse findUserById(UUID id) {
        return userMapper.toResponse(userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString())));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findUserByEmail(String email) {
        return userMapper.toResponse(userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("email=" + email)));
    }

}
