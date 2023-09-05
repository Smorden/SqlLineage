package com.siheng.cn.admin.core.kill;

import com.siheng.cn.admin.core.thread.JobTriggerPoolHelper;
import com.siheng.core.biz.model.ReturnT;
import com.siheng.core.util.Constants;
import com.siheng.core.util.ProcessUtil;

/**
 * flinkx-job trigger
 * Created by jingwk on 2019/12/15.
 */
public class KillJob {

    /**
     * @param logId
     * @param address
     * @param processId
     */
	public static ReturnT<String> trigger(String processId) {
		ReturnT<String> triggerResult = null;
		try {
			//将作业杀掉
			String cmdstr="";
			if(JobTriggerPoolHelper.isWindows()){
				cmdstr= Constants.CMDWINDOWTASKKILL+processId;
			}else {
				cmdstr=Constants.CMDLINUXTASKKILL+processId;
			}
			final Process process = Runtime.getRuntime().exec(cmdstr);
			String prcsId = ProcessUtil.getProcessId(process);
			triggerResult = new ReturnT<>(ReturnT.SUCCESS_CODE, "成功停止作业 !!!");
		}catch (Exception e) {
			triggerResult = new ReturnT<>(ReturnT.FAIL_CODE, null);
		}
		return triggerResult;
	}

}
