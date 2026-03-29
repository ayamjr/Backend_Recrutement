package com.recrutement.authservice.service;

import com.recrutement.authservice.dto.AuthResponse;
import com.recrutement.authservice.dto.LoginRequest;
import com.recrutement.authservice.dto.RegisterRequest;
import com.recrutement.authservice.entity.User;
import com.recrutement.authservice.enums.Role;
import com.recrutement.authservice.exception.InvalidCredentialsException;
import com.recrutement.authservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.recrutement.authservice.dto.UserResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.recrutement.authservice.entity.RefreshToken;
import com.recrutement.authservice.repository.RefreshTokenRepository;

import java.time.LocalDateTime;
import java.util.UUID;
import com.recrutement.authservice.entity.PasswordResetToken;
import com.recrutement.authservice.repository.PasswordResetTokenRepository;




@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;


    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService ,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository
                           ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.CANDIDATE)
                .enabled(true)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        RefreshToken refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .email(user.getEmail())
                .role(user.getRole().name())
                .refreshToken(refreshToken.getToken())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);
        RefreshToken refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .email(user.getEmail())
                .role(user.getRole().name())
                .refreshToken(refreshToken.getToken())
                .build();
    }
    @Override
    public UserResponse getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = (User) authentication.getPrincipal();

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .status(user.getStatus())
                .build();
    }

    private RefreshToken createRefreshToken(User user) {

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .user(user)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public AuthResponse refreshToken(String requestToken) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new IllegalArgumentException("Refresh token revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token expired");
        }

        User user = refreshToken.getUser();

        String newAccessToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(newAccessToken)
                .type("Bearer")
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
    @Override
    public void logout(String requestToken) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Override
    public String forgotPassword(String email) {

        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .user(user)
                .build();

        passwordResetTokenRepository.save(resetToken);

        return resetToken.getToken();
    }

    @Override
    public void resetPassword(String token, String newPassword) {

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Reset token already used");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset token expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }
}