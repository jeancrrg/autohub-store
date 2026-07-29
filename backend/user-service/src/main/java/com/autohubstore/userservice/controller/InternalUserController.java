package com.autohubstore.userservice.controller;

import com.autohubstore.userservice.controller.docs.InternalUserControllerDocs;
import com.autohubstore.userservice.domain.dto.response.UserResponse;
import com.autohubstore.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController implements InternalUserControllerDocs {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    @GetMapping("/credentials")
    public ResponseEntity<UserResponse> getCredentials(@RequestParam String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }
}
