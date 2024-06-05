package com.siheng.metadataplatform.controller;

import com.siheng.metadataplatform.dto.ResultDto;
import com.siheng.metadataplatform.service.DSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * @Author: ma.shuai
 * @CreateTime: 2024-06-04  17:45
 * @Description: ds相关操作的
 */
@Controller
@RequestMapping(value = "/ds")
@Slf4j
public class DSController {

    @Autowired
    private DSService dSService;

    @PostMapping(value = "init")
    public ResultDto initDsAllSqlLineage(@RequestBody Map<String, String> request) {
        try {
            dSService.initProcess(request.getOrDefault("branch", "release"));
            log.info("ds初始化完成 !!!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultDto.error(e.getMessage());
        }
        return ResultDto.success("初始化成功");
    }
}
