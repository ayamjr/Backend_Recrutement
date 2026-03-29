package com.recrutement.authservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String type;
    private String email;
    private String role;
    private String refreshToken;
}