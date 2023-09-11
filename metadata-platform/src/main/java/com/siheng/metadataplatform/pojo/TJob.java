package com.siheng.metadataplatform.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Dearest
 * @date 2023/9/8 2:10 下午
 * @Desc
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TJob {
    /**
     * 定时任务ID
     */
    private String jobId;
    /**
     * 定时任务名称
     */
    private String jobName;
    /**
     * 有效状态
     */
    private String status;
    /**
     * 可见状态
     */
    private String enableFlag;
    /**
     * cron表达式
     */
    private String cronExpression;
    /**
     * 调用目标字符串
     */
    private String executeTarget;


}
