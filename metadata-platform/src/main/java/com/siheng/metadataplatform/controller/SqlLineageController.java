package com.siheng.metadataplatform.controller;

import com.siheng.metadataplatform.dto.ResultDto;
import com.siheng.metadataplatform.service.SqlLineageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dearest
 * @date 2023/9/6 9:57 上午
 * @Desc
 */
@RestController
@RequestMapping("/sql/lineage")
@Slf4j
public class SqlLineageController {


    @Autowired
    private SqlLineageService sqlLineageService;

    @PostMapping(value = "/update")
    public ResultDto updateSqlLineage(@RequestBody Map<String, Object> request) {
//        Set<String> addAndModifyTableDirs = new HashSet<>();
//        if (request.containsKey("commits")) {
//            ObjectMapper objectMapper = new ObjectMapper();
//            List<Map<String, Object>> commits = objectMapper.convertValue(request.get("commits"), List.class);
//
//            for (Map<String, Object> commit : commits) {
//                List<String> added = objectMapper.convertValue(commit.get("added"), List.class);
//                List<String> modified = objectMapper.convertValue(commit.get("modified"), List.class);
//
//                addAndModifyTableDirs.addAll(added);
//                addAndModifyTableDirs.addAll(modified);
//            }
//        }


        return null;
//        return ResultDto.success(map);
    }


//    @PostMapping(value = "/update")
//    public String sayHello(@RequestBody Map<Object, Object> map) {
//        System.out.println(map);
//
//
//        return "Hello, World!";
//    }


    @PostMapping(value = "/init")
    public ResultDto initAllSqlLineage(@RequestBody Map<String, Object> request) {

        if (!request.containsKey("branch") || request.get("branch") == null) return ResultDto.error("请传入正确的分支名称");
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
