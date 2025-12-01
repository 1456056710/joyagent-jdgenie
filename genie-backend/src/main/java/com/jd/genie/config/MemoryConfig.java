package com.jd.genie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "memory.config")
public class MemoryConfig {
    private String storagePath;
    private int maxCapacity;
    private double importanceRatio;
    private double decayFactor;

    private int workingMemoryCapacity;
    private int workingMemoryTokens;
    private int workingMemoryTtlMinutes;

    private String[] perceptualMemoryModalities = {"text", "image", "audio", "video"};


}
