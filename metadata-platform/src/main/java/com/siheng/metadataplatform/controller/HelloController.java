package com.siheng.metadataplatform.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class HelloController {

    @PostMapping(value = "/test_webhook")
    public String sayHello(@RequestBody Map<Object, Object> map) {
        System.out.println(map);


        return "Hello, World!";
    }
}