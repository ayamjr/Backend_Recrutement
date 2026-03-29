package com.recrutement.authservice.service;

import com.recrutement.authservice.dto.AuthResponse;
import com.recrutement.authservice.dto.LoginRequest;
import com.recrutement.authservice.dto.RegisterRequest;
import com.recrutement.authservice.dto.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
    UserResponse getCurrentUser();
    AuthResponse refreshToken(String refreshToken);
    void logout(String refreshToken);
    String forgotPassword(String email);
    void resetPassword(String token, String newPassword);

}