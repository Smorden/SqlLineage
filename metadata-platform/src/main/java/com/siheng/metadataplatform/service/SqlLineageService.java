package com.siheng.metadataplatform.service;

import java.util.Map;

/**
 * @author Dearest
 * @date 2023/9/7 10:11 上午
 * @Desc
 */
public interface SqlLineageService {



    void updateSqlLineage(Map<String, String> map)throws Exception;

    void initAllSqlLineage(String branch) throws Exception;

    void initDsAllSqlLineage(String branch) throws Exception;
}
