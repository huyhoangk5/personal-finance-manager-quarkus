package com.finance.pfm.config;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;

/**
 * RateLimitService — sử dụng Redis INCR + EXPIRE để implement
 * sliding window counter cho rate limiting.
 *
 * Cơ chế:
 *   - Mỗi request: INCR key (tạo nếu chưa có)
 *   - Nếu là request đầu tiên (count == 1): SET EXPIRE = windowSeconds
 *   - Nếu count > limit: từ chối request → trả về false
 */
@ApplicationScoped
public class RateLimitService {

    private static final Logger LOG = Logger.getLogger(RateLimitService.class);

    @Inject
    RedisDataSource redisDataSource;

    /**
     * Kiểm tra xem request có được phép hay không.
     *
     * @param key           Redis key (vd: "rate:login:127.0.0.1")
     * @param limit         Số request tối đa trong window
     * @param windowSeconds Độ dài cửa sổ thời gian (giây)
     * @return true nếu request được phép, false nếu vượt giới hạn
     */
    public boolean isAllowed(String key, int limit, long windowSeconds) {
        try {
            ValueCommands<String, Long> commands = redisDataSource.value(Long.class);
            KeyCommands<String> keyCommands = redisDataSource.key();

            // INCR — tăng counter, tự tạo key với value 0 nếu chưa có
            Long currentCount = commands.incr(key);

            if (currentCount == null) {
                // Redis không available — fail open (cho phép request)
                LOG.warn("Redis không phản hồi, rate limit bị bỏ qua cho key: " + key);
                return true;
            }

            // Nếu đây là request đầu tiên trong window, set TTL
            if (currentCount == 1) {
                keyCommands.expire(key, Duration.ofSeconds(windowSeconds));
            }

            if (currentCount > limit) {
                LOG.debugf("Rate limit exceeded: key=%s count=%d limit=%d", key, currentCount, limit);
                return false;
            }

            return true;

        } catch (Exception e) {
            // Fail open: nếu Redis lỗi, không block user
            LOG.warnf("Lỗi Redis khi kiểm tra rate limit (key=%s): %s", key, e.getMessage());
            return true;
        }
    }
}
