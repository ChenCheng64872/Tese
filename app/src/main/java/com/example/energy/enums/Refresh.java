package com.example.energy.enums;

public enum Refresh {
    FPS30(30.0), FPS60(60.0), FPS120(120.0);
    public final double value;

    Refresh(double i) {
        this.value = i;
    }
}

