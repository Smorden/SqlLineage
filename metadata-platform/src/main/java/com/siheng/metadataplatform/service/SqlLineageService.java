package com.siheng.metadataplatform.service;

import java.util.Map;

/**
 * @author Dearest
 * @date 2023/9/7 10:11 上午
 * @Desc
 */
public interface SqlLineageService {
    int updateSqlLineage(Map<Object, Object> map);
}
