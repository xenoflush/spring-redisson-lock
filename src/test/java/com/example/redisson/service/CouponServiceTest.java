package com.example.redisson.service;

import com.example.redisson.domain.Coupon;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
class CouponServiceTest {

    @Autowired
    private CouponService couponService;
    private String couponKey;

    @BeforeEach
    void couponSetting() {
        final String name = "name";
        final String code = "Q2LJ5GZ52UNWOUND9BV";
        final int quantity = 80;
        final Coupon coupon = new Coupon(name, code, quantity);

        this.couponKey = couponService.keyResolver(coupon.getCode());
        couponService.setUsableCoupon(this.couponKey, quantity);
    }

    //락사용함_회원_50명_쿠폰_사용
    @Test
    @Order(1)
    void lockUse() throws InterruptedException{
        final int numberOfMember = 100;
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfMember);

        List<Thread> threadList = Stream
                .generate(() -> new Thread(new UsingLockMember(this.couponKey, countDownLatch)))
                .limit(numberOfMember)
                .collect(Collectors.toList());

        threadList.forEach(Thread::start);
        countDownLatch.await();
    }

    //락사용안함_회원_50명_쿠폰_사용
    @Test
    @Order(2)
    void lockNotUse() {
        final int numberOfMember = 100;
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfMember);

        List<Thread> threadList = Stream
                .generate(() -> new Thread(new NoLockMember(this.couponKey, countDownLatch)))
                .limit(numberOfMember)
                .collect(Collectors.toList());

        threadList.forEach(Thread::start);
    }

    private class UsingLockMember implements Runnable {
        private final String couponKey;
        private final CountDownLatch countDownLatch;

        public UsingLockMember(String couponKey, CountDownLatch countDownLatch) {
            this.couponKey = couponKey;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            couponService.useCouponWithLock(this.couponKey);
            countDownLatch.countDown();
        }
    }

    private class NoLockMember implements Runnable {
        private final String couponKey;
        private final CountDownLatch countDownLatch;

        public NoLockMember(String couponKey, CountDownLatch countDownLatch) {
            this.couponKey = couponKey;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            couponService.useCouponNoLock(this.couponKey);
            countDownLatch.countDown();
        }
    }
}