package com.example.ForDay.infra.s3.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloud.aws")
@Getter
@Setter
public class S3Properties {
    private String accessKey;
    private String secretKey;
    private String region;
    private String bucket;
    private String endpoint;
}