package com.usergrowth.api.common;

import lombok.Getter;

@Getter
public enum TaskStatusEnum {
    AVAILABLE("AVAILABLE", "可完成"),
    COMPLETED("COMPLETED", "已完成"),
    UNAVAILABLE("UNAVAILABLE", "不可完成");

    private final String code;
    private final String desc;

    TaskStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
