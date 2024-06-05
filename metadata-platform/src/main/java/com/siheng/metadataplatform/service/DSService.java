package com.siheng.metadataplatform.service;

import java.util.Map;

/**
 * @Author: ma.shuai
 * @CreateTime: 2024-06-05  09:45
 * @Description: 对ds的逻辑处理
 */
public interface DSService {

    void updateProcess(Map<String, String> map)throws Exception;

    void initProcess(String branch) throws Exception;
}
