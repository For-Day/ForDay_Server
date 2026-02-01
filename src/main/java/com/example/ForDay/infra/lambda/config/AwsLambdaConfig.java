package com.example.ForDay.infra.lambda.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

@Configuration
public class AwsLambdaConfig {

    @Value("${aws.region:ap-northeast-2}")
    private String region;

    @Bean
    public LambdaClient lambdaClient() {
        return LambdaClient.builder()
                .region(Region.AP_NORTHEAST_2)
                // 필요한 경우 CredentialsProvider 설정 추가
                .build();
    }
}