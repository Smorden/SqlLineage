package com.siheng.cn.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.siheng.cn.admin.dto.FlinkXJsonBuildDto;
import com.siheng.cn.admin.entity.JobDatasource;
import com.siheng.cn.admin.service.FlinkxJsonService;
import com.siheng.cn.admin.service.JobDatasourceService;
import com.siheng.cn.admin.tool.flinkx.FlinkxJsonHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 *
 * @Author: LarkMidTable
 * @Date: 2020/9/16 11:14
 * @Description:  JSON构建实现类
 **/
@Service
public class FlinkxJsonServiceImpl implements FlinkxJsonService {

    @Resource
    private JobDatasourceService jobJdbcDatasourceService;

    @Override
    public String buildJobJson(FlinkXJsonBuildDto FlinkXJsonBuildDto) {
        FlinkxJsonHelper flinkxJsonHelper = new FlinkxJsonHelper();
        // reader
        JobDatasource readerDatasource = jobJdbcDatasourceService.getById(FlinkXJsonBuildDto.getReaderDatasourceId());
        flinkxJsonHelper.initReader(FlinkXJsonBuildDto, readerDatasource);
        // writer
        JobDatasource writerDatasource = jobJdbcDatasourceService.getById(FlinkXJsonBuildDto.getWriterDatasourceId());
        flinkxJsonHelper.initWriter(FlinkXJsonBuildDto, writerDatasource);

        return JSON.toJSONString(flinkxJsonHelper.buildJob());
    }
}
