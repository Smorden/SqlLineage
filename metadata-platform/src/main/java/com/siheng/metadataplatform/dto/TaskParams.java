package com.siheng.metadataplatform.dto;

import lombok.Data;

/**
 * @Author: ma.shuai
 * @CreateTime: 2024-06-21  11:22
 * @Description: TODO
 */
@Data
public class TaskParams {
    private String rawScript;

    public TaskParams(String rawScript) {
        this.rawScript = rawScript;
    }
}
