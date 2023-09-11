package com.siheng.metadataplatform.controller;

import com.siheng.metadataplatform.dto.ResultDto;
import com.siheng.metadataplatform.enums.ResultCode;
import com.siheng.metadataplatform.service.SqlLineageService;
import com.siheng.metadataplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class HelloController {

    @Autowired
    private UserService userService;


    @Autowired
    private SqlLineageService sqlLineageService;


    @PostMapping(value = "/test_webhook")
    public ResultDto sayHello(@RequestBody Map<Object, Object> map) {

        try {
            if (map.containsKey("commits")) {
                int i = sqlLineageService.updateSqlLineage(map);
                if (i == 0) return ResultDto.success();
            } else {
                return ResultDto.success(ResultCode.SUCCESS_NOT_DATA.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultDto.error(e.getMessage());
        }

        return null;
//        return userService.queryUser();
    }


    @GetMapping(value = "/testDb")
    public String testDb(@RequestBody Map<String, Object> map) {
//        System.out.println(map);
//        String s = userService.queryUser();

        return null;
    }

}