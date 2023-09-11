package com.siheng.metadataplatform.mapper.first;

import com.siheng.metadataplatform.pojo.TJob;

import java.util.List;


/**
 * @author Dearest
 * @date 2023/9/8 2:13 下午
 * @Desc
 */
public interface TJobMapper {

    /**
     * 获取所有定时任务信息
     * @return
     */
    List<TJob> getAllJobList();

}
