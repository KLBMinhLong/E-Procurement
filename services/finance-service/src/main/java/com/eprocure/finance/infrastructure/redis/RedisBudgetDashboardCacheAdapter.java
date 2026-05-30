package com.eprocure.finance.infrastructure.redis;

import com.eprocure.finance.application.port.out.BudgetDashboardCachePort;
import com.eprocure.finance.application.service.BudgetDashboardView;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisBudgetDashboardCacheAdapter implements BudgetDashboardCachePort {
    private static final Logger log = LogManager.getLogger(RedisBudgetDashboardCacheAdapter.class);
    private static final String PREFIX = "finance:budget-dashboard:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisBudgetDashboardCacheAdapter(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${eprocure.finance.budget-cache-ttl-minutes:5}") long ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    @Override
    public Optional<BudgetDashboardView> findByBudgetId(UUID budgetId) {
        String key = key(budgetId);
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                log.debug("[CACHE] miss | key=finance:budget-dashboard:{}", LogMaskingUtil.maskId(budgetId));
                return Optional.empty();
            }
            log.debug("[CACHE] hit | key=finance:budget-dashboard:{}", LogMaskingUtil.maskId(budgetId));
            return Optional.of(objectMapper.readValue(value, BudgetDashboardView.class));
        } catch (RuntimeException exception) {
            log.warn("[CACHE] miss budget dashboard | key={} | reason={}",
                    LogMaskingUtil.maskId(budgetId),
                    exception.getClass().getSimpleName());
            return Optional.empty();
        } catch (Exception exception) {
            redisTemplate.delete(key);
            log.warn("[CACHE] evict budget dashboard | key={} | reason={}",
                    LogMaskingUtil.maskId(budgetId),
                    exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public void store(BudgetDashboardView dashboard) {
        try {
            redisTemplate.opsForValue().set(
                    key(dashboard.id()),
                    objectMapper.writeValueAsString(dashboard),
                    ttl);
            log.debug("[CACHE] put | key=finance:budget-dashboard:{} | ttl={}s",
                    LogMaskingUtil.maskId(dashboard.id()),
                    ttl.toSeconds());
        } catch (Exception exception) {
            log.warn("[CACHE] put budget dashboard failed | key={} | reason={}",
                    LogMaskingUtil.maskId(dashboard.id()),
                    exception.getClass().getSimpleName());
        }
    }

    @Override
    public void evict(UUID budgetId) {
        try {
            redisTemplate.delete(key(budgetId));
            log.debug("[CACHE] evict | key=finance:budget-dashboard:{}", LogMaskingUtil.maskId(budgetId));
        } catch (RuntimeException exception) {
            log.warn("[CACHE] evict budget dashboard failed | key={} | reason={}",
                    LogMaskingUtil.maskId(budgetId),
                    exception.getClass().getSimpleName());
        }
    }

    private String key(UUID budgetId) {
        return PREFIX + budgetId;
    }
}
