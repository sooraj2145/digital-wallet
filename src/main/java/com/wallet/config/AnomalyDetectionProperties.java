package com.wallet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "anomaly")
public class AnomalyDetectionProperties {

    private BigDecimal largeAmountThreshold = new BigDecimal("50000");
    private BigDecimal highAmountThreshold = new BigDecimal("10000");
    private int frequencyThreshold = 5;
    private int frequencyWindowMinutes = 60;

    public BigDecimal getLargeAmountThreshold() {
        return largeAmountThreshold;
    }

    public void setLargeAmountThreshold(
            BigDecimal largeAmountThreshold
    ) {
        this.largeAmountThreshold = largeAmountThreshold;
    }

    public BigDecimal getHighAmountThreshold() {
        return highAmountThreshold;
    }

    public void setHighAmountThreshold(
            BigDecimal highAmountThreshold
    ) {
        this.highAmountThreshold = highAmountThreshold;
    }

    public int getFrequencyThreshold() {
        return frequencyThreshold;
    }

    public void setFrequencyThreshold(int frequencyThreshold) {
        this.frequencyThreshold = frequencyThreshold;
    }

    public int getFrequencyWindowMinutes() {
        return frequencyWindowMinutes;
    }

    public void setFrequencyWindowMinutes(
            int frequencyWindowMinutes
    ) {
        this.frequencyWindowMinutes = frequencyWindowMinutes;
    }
}