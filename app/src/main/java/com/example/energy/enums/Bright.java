package com.example.energy.enums;

public enum Bright { FULL(255), THREEQ(255*2/3), HALF(255/2), QUART(255/4), ZERO(0);

    public final int value;
    Bright(int i) {
        this.value = i;
    }
}