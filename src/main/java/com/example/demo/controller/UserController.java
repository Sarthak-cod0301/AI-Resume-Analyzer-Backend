// controller/UserController.java
package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private String currentUserId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getProfile(currentUserId(authentication)));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateProfile(@Valid @RequestBody UpdateProfileRequestDTO request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(userService.updateProfile(currentUserId(authentication), request));
    }

    @PutMapping("/me/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request,
                                                                Authentication authentication) {
        userService.changePassword(currentUserId(authentication), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
 // controller/UserController.java — add this to your existing controller

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @Valid @RequestBody DeleteAccountRequestDTO request,
            Authentication authentication) {
        userService.deleteAccount(currentUserId(authentication), request);
        return ResponseEntity.ok(Map.of("message", "Your account and all associated data have been permanently deleted"));
    } 
}