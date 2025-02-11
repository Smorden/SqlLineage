package com.siheng.metadataplatform.mapper.first;

import com.siheng.metadataplatform.pojo.TableInfo;

import java.util.List;

/**
 * @Author: ma.shuai
 * @CreateTime: 2024-07-09  13:49
 * @Description: tableInfo相关操作
 */
public interface TableInfoMapper {
    List<TableInfo> queryAll();

    TableInfo queryByTaskName(String taskName);
    void save(TableInfo tableInfo);

}
