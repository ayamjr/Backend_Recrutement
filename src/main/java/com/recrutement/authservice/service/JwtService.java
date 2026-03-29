package com.recrutement.authservice.service;

import com.recrutement.authservice.entity.User;

public interface JwtService {

    String generateToken(User user);

    String extractUsername(String token);

    boolean isTokenValid(String token, User user);

}