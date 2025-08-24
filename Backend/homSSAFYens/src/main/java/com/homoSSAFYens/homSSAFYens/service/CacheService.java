package com.homoSSAFYens.homSSAFYens.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CacheService {

    private final StringRedisTemplate redis;
    private final ObjectMapper om;

    public CacheService(StringRedisTemplate redis, ObjectMapper om) {
        this.redis = redis;
        this.om = om;
    }

    // 단일 객체
    public <T> T get(String key, Class<T> type) {
        String json = redis.opsForValue().get(key);
        if (json == null) return null;
        try { return om.readValue(json, type); }
        catch (Exception e) { return null; }
    }

    // 리스트/제네릭 컬렉션 등
    public <T> T get(String key, com.fasterxml.jackson.core.type.TypeReference<T> typeRef) {
        String json = redis.opsForValue().get(key);
        if (json == null) return null;
        try { return om.readValue(json, typeRef); }
        catch (Exception e) { return null; }
    }

    // 저장 (+5% 지터)
    public void set(String key, Object value, Duration ttl) {
        try {
            String json = om.writeValueAsString(value);
            redis.opsForValue().set(key, json, jitter(ttl));
        } catch (Exception ignore) {}
    }

    // 0~5% 가산 지터
    public Duration jitter(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) return ttl;
        long sec = ttl.getSeconds();
        long delta = Math.max(1, Math.round(sec * 0.05));         // 5%
        long add = ThreadLocalRandom.current().nextLong(0, delta + 1);
        return Duration.ofSeconds(sec + add);
    }

    /** 빈 결과(없음)도 잠깐 캐시해 요청 폭주 방지 */
    public void setNull(String key, Duration ttl) {
        redis.opsForValue().set(key, "__NULL__", ttl);
    }
    public boolean isNull(String key) {
        String v = redis.opsForValue().get(key);
        return "__NULL__".equals(v);
    }

    /** 간단 락 (5초 등 짧게) */
    public boolean tryLock(String key, Duration hold) {
        String lockKey = "lock:" + key;
        Boolean ok = redis.opsForValue().setIfAbsent(lockKey, "1", hold);
        return Boolean.TRUE.equals(ok);
    }

}
