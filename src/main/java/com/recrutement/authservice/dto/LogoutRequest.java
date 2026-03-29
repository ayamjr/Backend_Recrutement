package com.recrutement.authservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {

    private String refreshToken;
}