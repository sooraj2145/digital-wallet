package com.wallet.service;


import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    public Long getCurrentUserId(Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();

        return Long.valueOf(jwt.getSubject());
    }
}
