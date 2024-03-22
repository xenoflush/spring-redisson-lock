package com.example.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class CouponService {
    private final RedissonClient redissonClient;
    private final int EMPTY = 0;

    public String keyResolver(String code) {
        return "COUPON:" + code;
    }

    public void useCouponWithLock(final String key) {
        final String lockName = key + ":lock";
        final RLock lock = redissonClient.getLock(lockName);
        final String threadName = Thread.currentThread().getName();

        try {
            if (!lock.tryLock(1, 3, TimeUnit.SECONDS)) {
                return;
            }

            final int quantity = usableCoupon(key);
            if (quantity <= EMPTY) {
                log.info("threadName : {} / 사용 가능 쿠폰 모두 소진", threadName);
                return;
            }

            log.info("threadName : {} / 사용 가능 쿠폰 수량 : {}개", threadName, quantity);
            setUsableCoupon(key, quantity - 1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (lock != null && lock.isLocked()) {
                lock.unlock();
            }
        }
    }

    public void useCouponNoLock(final String key) {
        final String threadName = Thread.currentThread().getName();
        final int quantity = usableCoupon(key);

        if (quantity <= EMPTY) {
            log.info("threadName : {} / 사용 가능 쿠폰 모두 소진", threadName);
            return;
        }

        log.info("threadName : {} / 사용 가능 쿠폰 수량 : {}개", threadName, quantity);
        setUsableCoupon(key, quantity - 1);
    }

    public void setUsableCoupon(String key, int quantity) {
        redissonClient.getBucket(key).set(quantity);
    }

    public int usableCoupon(String key) {
        return (int) redissonClient.getBucket(key).get();
    }
}
