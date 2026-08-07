package com.autohubstore.userservice.controller;

import com.autohubstore.userservice.controller.docs.InternalUserControllerDocs;
import com.autohubstore.userservice.domain.dto.response.UserResponse;
import com.autohubstore.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController implements InternalUserControllerDocs {

    private final UserService userService;

    @GetMapping("/credentials")
    public ResponseEntity<UserResponse> getCredentials(@RequestParam String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

}
