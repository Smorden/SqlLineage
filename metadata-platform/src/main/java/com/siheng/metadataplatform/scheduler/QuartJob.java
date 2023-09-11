package com.siheng.metadataplatform.scheduler;
import cn.hutool.core.date.DateUtil;
import com.siheng.metadataplatform.pojo.TJob;
import com.siheng.metadataplatform.utils.InvokeUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
/**
 * @author Dearest
 * @date 2023/9/8 2:18 下午
 * @Desc
 */


import java.util.Date;

public class QuartJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            before(context);
            doExecute(context);
            after(context, null);
        } catch (Exception e) {
            System.out.println("定时任务执行失败：" + DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            after(context, e);
        }
    }

    /**
     * 执行前
     *
     * @param context 工作执行上下文对象
     */
    protected void before(JobExecutionContext context) {
        System.out.println("------------");
        System.out.println("开始执行定时任务：" + DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 执行后
     *
     * @param context 工作执行上下文对象
     */
    protected void after(JobExecutionContext context, Exception e) {
        System.out.println("定时任务执行结束:" + DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 执行方法
     *
     * @param context 工作执行上下文对象
     * @throws Exception 执行过程中的异常
     */
    protected void doExecute(JobExecutionContext context) throws Exception {
        //获取JobDetail中关联的数据
        TJob job = (TJob) context.getJobDetail().getJobDataMap().get("job");
        System.out.println("调用目标字符串");
        //立即执行
        InvokeUtil.invokeMethod(job);
        System.out.println("当前时间 :" + DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + "\n任务名(" + job.getJobName()+")");
    }
}