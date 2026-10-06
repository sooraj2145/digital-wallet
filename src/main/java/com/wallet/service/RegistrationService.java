package com.wallet.service;

import com.wallet.dto.RegisterRequest;
import com.wallet.entity.User;
import com.wallet.entity.UserCredential;
import com.wallet.exception.EmailAlreadyRegisteredException;
import com.wallet.repository.UserCredentialRepository;
import com.wallet.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            UserRepository userRepository,
            UserCredentialRepository userCredentialRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException(
                    "Email is already registered"
            );
        }

        User user = userRepository.save(
                new User(
                        request.name(),
                        request.email()
                )
        );

        String passwordHash =
                passwordEncoder.encode(request.password());

        UserCredential credential =
                new UserCredential(
                        user,
                        passwordHash
                );

        userCredentialRepository.save(credential);

        return user;
    }
}