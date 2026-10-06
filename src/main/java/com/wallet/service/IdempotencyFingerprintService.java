package com.wallet.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class IdempotencyFingerprintService {

    public String fingerprintTransfer(
            Long sourceWalletId,
            Long destinationWalletId,
            BigDecimal amount,
            String currency
    ) {

        String canonicalRequest =
                sourceWalletId + "|" +
                        destinationWalletId + "|" +
                        amount.stripTrailingZeros().toPlainString() + "|" +
                        currency;

        return sha256(canonicalRequest);
    }

    private String sha256(String value) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(
                        String.format("%02x", b)
                );
            }

            return result.toString();

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }

    public String fingerprintDeposit(
            Long walletId,
            BigDecimal amount,
            String currency
    ) {

        String canonicalRequest =
                walletId + "|" +
                        amount.stripTrailingZeros().toPlainString() + "|" +
                        currency;

        return sha256(canonicalRequest);
    }
}