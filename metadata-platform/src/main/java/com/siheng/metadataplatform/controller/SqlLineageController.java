package com.siheng.metadataplatform.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.siheng.metadataplatform.dto.ResultDto;
import com.siheng.metadataplatform.service.SqlLineageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author Dearest
 * @date 2023/9/6 9:57 上午
 * @Desc
 * 1.如果是删除表---->标记@下线即可
 * 2.如果是改表名---->标记原文件下线,新增一个新文件
 * 3.一个文件只能有一个insert ... select
 */
@RestController
@RequestMapping("lineage")
@Slf4j
public class SqlLineageController {


    @Autowired
    private SqlLineageService sqlLineageService;

    @PostMapping(value = "/update")
    public ResultDto updateSqlLineage(@RequestBody Map<String, String> request) {
        try {
            sqlLineageService.updateSqlLineage(request);
        } catch (Exception e) {
            return ResultDto.error(e.getMessage());
        }
        return ResultDto.success("初始化成功");
    }

    @PostMapping(value = "/callback")
    public ResultDto callbackSqlLineage(@RequestBody String request) {
        try {

//            if(StringUtils.isNoneEmpty(request)){
//                JSONObject baseJson = JSON.parseObject(request);
//                JSONObject objectAttributes = baseJson.getJSONObject("object_attributes");
//                if("merged".equals(objectAttributes.getString("state"))){
//                    String targetBranch = objectAttributes.getString("target_branch");
//
////            sqlLineageService.updateSqlLineage(request);
//                    System.out.println(request);
//                    log.info("ds初始化完成 !!!");
//                }
//            }
        } catch (Exception e) {
            return ResultDto.error(e.getMessage());
        }
        return ResultDto.success("初始化成功");
    }

    @PostMapping(value = "/init")
    public ResultDto initAllSqlLineage(@RequestBody Map<String, Object> request) {

        if (!request.containsKey("branch") || request.get("branch") == null)
            return ResultDto.error("请传入正确的分支名称");
        try {
            String branch = request.get("branch").toString();
            sqlLineageService.initAllSqlLineage(branch);
            log.info("初始化完成 !!!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultDto.error(e.getMessage());
        }
        return ResultDto.success("初始化成功");
    }
}
