package com.siheng.metadataplatform.dto;

import lombok.Data;

/**
 * @Author: ma.shuai
 * @CreateTime: 2024-06-21  11:17
 * @Description: 任务创建模板
 */
@Data
public class TaskDefinitionDto {
    private Long code;

    private String delayTime = "0";

    private Integer environmentCode = -1;

    private String failRetryInterval = "5";

    private Integer failRetryTimes = 5;

    private String flag = "YES";

    private String name;

    private TaskParams taskParams;

    private String taskPriority = "MEDIUM";

    private String taskType = "SHELL";

    private Integer timeout = 0;

    private String timeoutFlag = "CLOSE";

    private String workerGroup = "default";

    public TaskDefinitionDto(Long code, String name, String rawScript) {
        this.code = code;
        this.name = name;
        this.taskParams = new TaskParams(rawScript);
    }
}
