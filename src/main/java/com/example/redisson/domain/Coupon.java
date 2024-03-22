package com.example.redisson.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Coupon {

    private String name;
    private String code;
    private int quantity;
}
