package com.siheng.metadataplatform.controller;

import com.siheng.metadataplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class HelloController {

    @Autowired
    private UserService userService;

    @PostMapping(value = "/test_webhook")
    public String sayHello(@RequestBody Map<Object, Object> map) {
        System.out.println(map);


        return userService.queryUser();
    }


    @GetMapping(value = "/testDb")
    public String testDb(@RequestBody Map<Object, Object> map) {
//        System.out.println(map);
        String s = userService.queryUser();

        return s;
    }

}