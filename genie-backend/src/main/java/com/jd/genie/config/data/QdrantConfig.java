package com.jd.genie.config.data;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

@Data
public class QdrantConfig {
    private Boolean enable;
    private String host;
    private int port;
    private String apiKey;
    private String embeddingUrl;
    private String model;
    private String embeddingApiKey;
    private String collection;
}
