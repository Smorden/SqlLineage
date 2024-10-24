package com.siheng.metadataplatform.dto;

import lombok.Data;

/**
 * @Author: ma.shuai
 * @CreateTime: 2024-06-24  13:12
 * @Description: 用户流程中的全局参数定义
 */
@Data
public class GlobalParamsDto {
    private String prop;
    private String direct="IN";
    private String type="VARCHAR";
    private String value;

    public GlobalParamsDto(String prop, String value) {
        this.prop = prop;
        this.value = value;
    }

    public GlobalParamsDto(String prop, String direct, String type, String value) {
        this.prop = prop;
        this.direct = direct;
        this.type = type;
        this.value = value;
    }
}
