package com.wallet;

import com.wallet.config.AnomalyDetectionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AnomalyDetectionProperties.class)
public class DigitalWalletApplication {

	public static void main(String[] args) {

		SpringApplication.run(DigitalWalletApplication.class, args);
	}

}
