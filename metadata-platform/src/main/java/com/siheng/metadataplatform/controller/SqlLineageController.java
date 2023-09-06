package com.siheng.metadataplatform.controller;

import com.siheng.metadataplatform.dto.ResultDto;
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
@RequestMapping("/sql-lineage")
public class SqlLineageController {


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


}
