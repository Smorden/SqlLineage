package com.siheng.metadataplatform.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.siheng.metadataplatform.dto.ResultDto;
import com.siheng.metadataplatform.service.DSService;
import com.siheng.metadataplatform.utils.SqlLineageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.siheng.metadataplatform.constant.CustomConstant.*;

/**
 * @Author: ma.shuai
 * @CreateTime: 2024-06-04  17:45
 * @Description: ds相关操作的
 */
@RestController
@RequestMapping(value = "ds")
@Slf4j
public class DSController {

    @Autowired
    private DSService dSService;

    @PostMapping(value = "/init")
    public ResultDto initDsAllSqlLineage(@RequestBody Map<String, String> request) {
        try {
            dSService.initProcess(request.getOrDefault(branch, test));
            log.info("ds初始化完成 !!!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultDto.error(e.getMessage());
        }
        return ResultDto.success("初始化成功");
    }

    @PostMapping(value = "/update")
    public ResultDto updateDsSqlLineage(@RequestBody String request) {
        try {
            if (StringUtils.isNoneEmpty(request)) {
                JSONObject baseJson = JSON.parseObject(request);
                JSONObject objectAttributes = baseJson.getJSONObject("object_attributes");
                String targetBranch = objectAttributes.getString("target_branch");
                if ("merged".equals(objectAttributes.getString("state")) && test.equals(targetBranch)) {
                    String mergeCommitSha = objectAttributes.getString("merge_commit_sha");
                    SqlLineageUtil.queue.offer(mergeCommitSha);
                    while (true){
                        if (SqlLineageUtil.queue.peek()==mergeCommitSha){
                            dSService.updateProcess(targetBranch, mergeCommitSha);
                            log.info("merge请求ds同步完成 !!!");
                            return ResultDto.success("merge请求ds同步完成 !!!");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultDto.error(e.getMessage());
        }
        return ResultDto.success("不在更新范围内 !!!");
    }

    @PostMapping(value = "/test")
    public ResultDto test(@RequestBody String request) {





      return   ResultDto.success("请求成功");
    }



}
