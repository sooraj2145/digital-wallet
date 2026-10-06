package com.wallet.controller;

import com.wallet.entity.Currency;
import com.wallet.entity.User;
import com.wallet.entity.Wallet;
import com.wallet.repository.UserRepository;
import com.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatementQaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void authenticatedUserCanAskStatementQuestion() {

        User user =
                userRepository.save(
                        new User(
                                "Statement User",
                                "statement-user@example.com"
                        )
                );

        Wallet wallet =
                walletRepository.save(
                        new Wallet(user, Currency.INR)
                );

        assertNotNull(user.getId());
        assertNotNull(wallet.getId());
    }

    @Test
    void statementEndpointRequiresAuthentication() throws Exception {

        mockMvc.perform(
                        post("/api/statement/ask")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "question": "What was my largest transaction?"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized());
    }
}