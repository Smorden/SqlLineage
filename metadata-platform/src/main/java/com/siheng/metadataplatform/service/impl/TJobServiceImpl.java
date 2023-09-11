package com.siheng.metadataplatform.service.impl;


import com.siheng.metadataplatform.mapper.first.TJobMapper;
import com.siheng.metadataplatform.pojo.TJob;
import com.siheng.metadataplatform.scheduler.QuartJob;
import com.siheng.metadataplatform.service.TJobService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Dearest
 * @date 2023/9/8 2:15 下午
 * @Desc
 */
@Service
public class TJobServiceImpl implements TJobService {
    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;
    @Autowired
    private TJobMapper jobMapper;

    /**
     * 初始化定时任务
     */
    @PostConstruct
    public void init() {
        //获取调度器
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        //获取所有定时任务
        List<TJob> jobList = jobMapper.getAllJobList();
        //定时任务不存在
        if (jobList == null || jobList.size() <= 0) {
            return;
        }
        //获取有效的定时任务
        jobList = jobList.stream().filter(x -> x.getStatus().equals("1")).collect(Collectors.toList());
        //没有需要创建的定时任务
        if (jobList == null || jobList.size() <= 0) {
            return;
        }

        try {
            //清空调动任务
            scheduler.clear();
            for (TJob job : jobList) {
                createJob(job,scheduler);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建定时任务
     *
     * @param job       定时任务
     * @param scheduler 调度器
     */
    public void createJob(TJob job, Scheduler scheduler) throws Exception {
        if (job == null) {
            return;
        }
        //触发器主键
        TriggerKey triggerKey = TriggerKey.triggerKey(job.getJobId(), null);
        // 表达式调度构建器
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
        //定时任务主键
        JobKey jobKey = JobKey.jobKey(job.getJobId(), null);
        // 构建job信息
        JobDetail jobDetail = JobBuilder.newJob(QuartJob.class).withIdentity(jobKey).build();
        //触发器
        CronTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).withSchedule(cronScheduleBuilder).build();
        // 放入参数，运行时的方法可以获取
        jobDetail.getJobDataMap().put("job", job);
        // 判断定时任务存在
        if (scheduler.checkExists(jobKey)) {
            // 定时任务已存在则重新设置触发器
            scheduler.rescheduleJob(triggerKey, cronTrigger);
//            scheduler.deleteJob()
        } else {
            // 定时任务不存在则创建定时任务
            scheduler.scheduleJob(jobDetail, cronTrigger);
        }
    }




}
