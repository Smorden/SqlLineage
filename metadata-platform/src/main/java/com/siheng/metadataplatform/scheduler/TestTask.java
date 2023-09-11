package com.siheng.metadataplatform.scheduler;

import org.springframework.stereotype.Component;

/**
 * @author Dearest
 * @date 2023/9/8 2:27 下午
 * @Desc
 */
@Component("testTask")
public class TestTask {
    /**
     * 定时任务调用方法
     *
     * @param jobId 定时任务ID
     */
    public void test(String jobId) {
        System.out.println("当前执行的定时任务ID：" + jobId);
    }
}