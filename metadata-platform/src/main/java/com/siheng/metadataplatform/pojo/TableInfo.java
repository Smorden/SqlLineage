package com.siheng.metadataplatform.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: ma.shuai
 * @CreateTime: 2024-07-09  13:54
 * @Description: table_info表实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableInfo {
    private String taskName;
    private String serverName;
    private String dbName;
    private String tableName;
    private String config;
    private Integer isEnable;
    private Integer initialFlag;
}
