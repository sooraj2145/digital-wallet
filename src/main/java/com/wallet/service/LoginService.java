package com.wallet.service;

import com.wallet.dto.LoginRequest;
import com.wallet.dto.LoginResponse;
import com.wallet.entity.User;
import com.wallet.entity.UserCredential;
import com.wallet.exception.AuthenticationFailedException;
import com.wallet.repository.UserCredentialRepository;
import com.wallet.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public LoginService(
            UserRepository userRepository,
            UserCredentialRepository userCredentialRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.email())
                .orElseThrow(() ->
                        new AuthenticationFailedException("Invalid email or password"));

        UserCredential credential = userCredentialRepository
                .findByUserId(user.getId())
                .orElseThrow(() ->
                        new AuthenticationFailedException("Invalid email or password"));

        if (!passwordEncoder.matches(
                request.password(),
                credential.getPasswordHash()
        )) {
            throw new AuthenticationFailedException(
                    "Invalid email or password"
            );
        }

        String token = jwtTokenService.generateToken(
                user.getId(),
                user.getEmail()
        );

        return new LoginResponse(token, "Bearer");
    }
}