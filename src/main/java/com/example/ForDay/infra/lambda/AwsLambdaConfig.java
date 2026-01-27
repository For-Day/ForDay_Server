package com.example.ForDay.infra.lambda;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

@Configuration
public class AwsLambdaConfig {

    @Bean
    public LambdaClient lambdaClient() {
        return LambdaClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();
    }
}
