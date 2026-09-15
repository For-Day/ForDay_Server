package com.example.ForDay.infra.lambda.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

import java.time.Duration;

@Configuration
public class AwsLambdaConfig {

    @Value("${aws.region:ap-northeast-2}")
    private String region;

    // CoverLambdaInvoker.invokeSync는 REQUEST_RESPONSE(동기) 호출이라, 콜드 스타트나 지연 시
    // 대기 시간에 상한이 없으면 트랜잭션 밖(#355 분리 이후에도)에서 호출자를 무한정 붙잡는다.
    // apiCallAttemptTimeout: 1회 시도 상한. 콜드 스타트를 감안해 넉넉히 둔다.
    @Value("${lambda.api-call-attempt-timeout-seconds:10}")
    private long apiCallAttemptTimeoutSeconds;

    // apiCallTimeout: 재시도까지 포함한 전체 상한. 클라이언트가 기다릴 수 있는 SLA 안에 들어오도록
    // attempt timeout보다 넉넉하되 과하게 길지 않게 둔다. 실제 콜드 스타트 분포(p99) 측정 후
    // 튜닝이 필요한 값이라 프로퍼티로 분리했다 - 기본값은 잠정치다.
    @Value("${lambda.api-call-timeout-seconds:20}")
    private long apiCallTimeoutSeconds;

    // 재시도 1회(총 2회 시도)가 기본값. Lambda 함수가 멱등(SET_COVER는 같은 srcKey/dstKey로
    // 재호출해도 결과가 같다)이라 재시도가 안전하다.
    @Value("${lambda.max-retries:1}")
    private int maxRetries;

    @Bean
    public LambdaClient lambdaClient() {
        ClientOverrideConfiguration overrideConfiguration = ClientOverrideConfiguration.builder()
                .apiCallAttemptTimeout(Duration.ofSeconds(apiCallAttemptTimeoutSeconds))
                .apiCallTimeout(Duration.ofSeconds(apiCallTimeoutSeconds))
                .retryPolicy(RetryPolicy.builder(RetryMode.STANDARD)
                        .numRetries(maxRetries)
                        .build())
                .build();

        return LambdaClient.builder()
                .region(Region.of(region))
                .overrideConfiguration(overrideConfiguration)
                // 필요한 경우 CredentialsProvider 설정 추가
                .build();
    }
}
