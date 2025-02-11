package com.siheng.metadataplatform.pojo;

import com.alibaba.fastjson.JSONObject;
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
    public String taskName;
    public String serverName;
    public String dbName;
    public String tableName;
    public JSONObject config;
    public Integer isEnable;
    public Integer initialFlag;
}
