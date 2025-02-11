package com.siheng.metadataplatform.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.siheng.metadataplatform.dto.ResultDto;
import com.siheng.metadataplatform.pojo.GlobalParams;
import com.siheng.metadataplatform.pojo.TableInfo;
import com.siheng.metadataplatform.service.DSService;
import com.siheng.metadataplatform.utils.SqlLineageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

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
    @Value("${siheng.gitlab.branch}")
    private String currentBranch;

    @Autowired
    private DSService dSService;

    @PostMapping(value = "/init")
    public ResultDto initDsAllSqlLineage() {
        try {
            dSService.initProcess(currentBranch);
            log.info("ds初始化完成 !!!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultDto.error(e.getMessage());
        }
        return ResultDto.success("初始化成功 !!!");
    }

    @PostMapping(value = "/update")
    public ResultDto updateDsSqlLineage(@RequestBody String request) {
        System.out.println("合并请求入参是--->");
        System.out.println(request);
        if (StringUtils.isNoneEmpty(request)) {
            JSONObject baseJson = JSON.parseObject(request);
            JSONObject objectAttributes = baseJson.getJSONObject("object_attributes");
            String targetBranch = objectAttributes.getString("target_branch");
            if ("merged".equals(objectAttributes.getString("state")) && currentBranch.equals(targetBranch)) {
                String mergeCommitSha = objectAttributes.getString("merge_commit_sha");
                String uniqueKey = StringUtils.join(targetBranch, commaStr, mergeCommitSha);
                SqlLineageUtil.queue.offer(uniqueKey);
                return ResultDto.success("merge请求ds加入队列完成 !!!");
            }
        }
        log.info("不在更新范围内 !!!");
        System.out.println("不在更新范围内 !!!");
        return ResultDto.success("保存成功 !!!");
    }


    @PostMapping(value = "/save")
    public ResultDto save(@RequestBody GlobalParams globalParams) throws Exception {
        return ResultDto.success(dSService.save(globalParams));
    }

    @GetMapping(value = "/pause")
    public ResultDto pause() {
        sharedData = !sharedData;
        return ResultDto.success("更新ds的依赖状态是:" + sharedData);
    }

    /*
     * @description:
     * @author: ma.shuai
     * @date: 2024/12/16 13:42
     * taskDependType：TASK_POST(默认),TASK_PRE,TASK_ONLY
     *
     **/
    @PostMapping(value = "/start/instance")
    public ResultDto startProcessInstance(@RequestBody Map<String, String> requestMap) throws Exception {
        if (StringUtils.isEmpty(requestMap.get(taskName))) {
            return ResultDto.success("请输入任务名称（task_name）!!!");
        }
        return ResultDto.success(JSON.parseObject(dSService.startProcessInstance(requestMap)));
    }

    @PostMapping(value = "/table/info")
    public ResultDto tableInfo(@RequestBody(required = false) TableInfo tableInfo) throws Exception {
        return ResultDto.success(dSService.getTableInfoByTaskName(tableInfo));
    }


}
