package com.siheng.metadataplatform.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author Dearest
 * @date 2023/9/8 11:05 上午
 * @Desc
 */
@Slf4j
public class SqlLineageJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        System.out.println("----任务开始执行");
        log.info("任务名：" + jobDetail.getKey().getName() + ",组名：" +
                jobDetail.getKey().getGroup() + "------执行的定时任务工作内容！");

    }
}
