package com.siheng.metadataplatform.enums;

import lombok.Data;

/**
 * @author Dearest
 * @date 2023/9/6 11:05 上午
 * @Desc
 */
public enum ResultCode {

    SUCCESS(200, "成功"),
    SUCCESS_NOT_DATA(200, "需要更新的数据"),
    ERROR(500, "服务器异常");

    private Integer code;
    private String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
