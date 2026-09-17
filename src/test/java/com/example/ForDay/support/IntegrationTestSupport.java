package com.example.ForDay.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;

/**
 * 스프링 컨텍스트를 띄우는 통합 테스트의 공통 베이스.
 *
 * <p>테스트는 H2 인메모리를 쓰므로 실행할 때마다 스키마가 새로 만들어지고 식별자가 1부터
 * 다시 시작한다. 반면 Redis는 실행 사이에 살아남기 때문에, {@code @Cacheable}이 남긴
 * 이전 실행의 값이 같은 키로 재사용된다(예: {@code activityRecord::1}). 그 값이 담고 있는
 * 사용자 ID는 이미 사라진 뒤라 다음 실행이 엉뚱하게 깨진다. 캐시 TTL이 3분이라
 * "방금 돌리면 실패하고 좀 있다 돌리면 통과하는" 형태로 나타난다.
 *
 * <p>그래서 매 테스트 전에 Redis를 비운다. 대상은 {@code application-test.yml}이 지정한
 * 테스트 전용 DB(1번)이며, 개발용 데이터가 있는 0번은 건드리지 않는다.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void flushTestRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
    }
}
