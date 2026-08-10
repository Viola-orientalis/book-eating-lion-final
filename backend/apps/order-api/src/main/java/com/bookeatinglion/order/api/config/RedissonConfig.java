package com.bookeatinglion.order.api.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 재고 차감 1차 방어선(Redlock). {@code common}이 아니라 여기 두는 이유는
 * 이게 서비스 간 계약이 아니라 order-service 로컬 동시성 문제이기 때문이다.
 *
 * 기존 {@code spring.data.redis.host/port} 를 그대로 재사용한다 — 캐시/Streams 와
 * 별개의 Redis 인스턴스를 새로 두지 않는다.
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + redisHost + ":" + redisPort);
        return Redisson.create(config);
    }
}
