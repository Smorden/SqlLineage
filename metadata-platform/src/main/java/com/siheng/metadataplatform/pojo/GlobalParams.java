package com.siheng.metadataplatform.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: ma.shuai
 * @CreateTime: 2024-06-24  13:12
 * @Description: 用户流程中的全局参数定义
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GlobalParams {
    private String processName="default";
    private String prop;
    private String direct="IN";
    private String type="VARCHAR";
    private String value;

}
