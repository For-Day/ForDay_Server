package com.example.ForDay.global.config.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(
        basePackages = "com.example.ForDay.domain.auth.repository"
)
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }


    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.profiles.active:local}") String profile
    ) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);

        // 배포 환경(blue, green)에서만 SSL 사용
        if (profile.equals("blue") || profile.equals("green")) {
            LettuceClientConfiguration clientConfig =
                    LettuceClientConfiguration.builder()
                            .useSsl()
                            .build();

            return new LettuceConnectionFactory(config, clientConfig);
        }

        return new LettuceConnectionFactory(config);
    }


}