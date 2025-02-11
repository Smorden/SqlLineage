package com.siheng.metadataplatform.mapper.first;

import com.siheng.metadataplatform.pojo.GlobalParams;
import com.siheng.metadataplatform.pojo.TableInfo;

import java.util.List;

/**
 * @Author: ma.shuai
 * @CreateTime: 2024-07-09  13:49
 * @Description: globalParams相关操作
 */
public interface GlobalParamsMapper {
    List<GlobalParams> queryAll();

    void save(GlobalParams globalParams);

}
