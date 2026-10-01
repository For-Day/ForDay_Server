package com.example.ForDay.global.measure.config;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.example.ForDay.infra.s3.property.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 이미지 리사이즈 측정(6번 사례) 전용. 프로덕션 {@code S3Config}는 정적 액세스키를
 * 쓰지만(로컬/CI 편의), 측정 EC2는 그 키를 넣지 않고 대신 인스턴스 역할 자격증명을 쓴다
 * - {@code forday-perf-target-role}에 {@code test_activity_record/*} 경로로만 좁힌
 * S3 권한을 붙여뒀다({@code ForDay_Infra/perf_test.tf}). {@code @Primary}로
 * {@code measure} 프로파일에서만 기존 {@code S3Config}의 빈을 덮어쓴다.
 */
@Configuration
@Profile("measure")
@RequiredArgsConstructor
public class MeasureS3Config {

    private final S3Properties s3Properties;

    @Bean
    @Primary
    public AmazonS3 measureAmazonS3() {
        // DefaultAWSCredentialsProviderChain은 아니다 - AWS SDK v1의 환경변수 프로바이더가
        // AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY뿐 아니라 레거시 이름인
        // AWS_ACCESS_KEY/AWS_SECRET_KEY도 인식하는데, 이 컨테이너의 .env가 바로 그 이름으로
        // 더미 값("dummy")을 넣어뒀다(정적 자격증명을 쓰는 기존 S3Config용). 체인을 쓰면
        // EC2 인스턴스 역할보다 그 더미 값이 먼저 선택돼버린다(실측 중 발견). 인스턴스
        // 프로파일 자격증명만 명시적으로 써서 이 충돌을 피한다.
        return AmazonS3ClientBuilder.standard()
                .withRegion(s3Properties.getRegion())
                .withCredentials(InstanceProfileCredentialsProvider.getInstance())
                .build();
    }
}
