package com.github.gitcat.spring.retryplus.enums;

public enum YesNoEnum {

    YES(1),
    NO(0);

    private final int value;

    YesNoEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
