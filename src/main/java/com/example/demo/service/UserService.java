// service/UserService.java
package com.example.demo.service;

import com.example.demo.dto.*;

public interface UserService {
    UserProfileDTO getProfile(String userId);
    UserProfileDTO updateProfile(String userId, UpdateProfileRequestDTO request);
    void changePassword(String userId, ChangePasswordRequestDTO request);
 // service/UserService.java — add this to your existing interface
    void deleteAccount(String userId, DeleteAccountRequestDTO request);
}