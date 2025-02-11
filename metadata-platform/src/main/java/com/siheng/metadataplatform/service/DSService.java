package com.siheng.metadataplatform.service;

import com.siheng.metadataplatform.pojo.GlobalParams;
import com.siheng.metadataplatform.pojo.TableInfo;

import java.util.List;
import java.util.Map;

/**
 * @Author: ma.shuai
 * @CreateTime: 2024-06-05  09:45
 * @Description: 对ds的逻辑处理
 */
public interface DSService {

    void updateProcess(String uniqueKey)throws Exception;

    void initProcess(String branch) throws Exception;

    List<GlobalParams> save(GlobalParams globalParams) throws Exception;

    String startProcessInstance(Map<String, String> requestMap)throws Exception;
    TableInfo getTableInfoByTaskName(TableInfo tableInfo) throws Exception;


}
