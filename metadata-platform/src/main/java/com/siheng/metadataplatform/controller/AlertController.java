package com.siheng.metadataplatform.controller;

import com.siheng.metadataplatform.dto.ResultDto;
import com.siheng.metadataplatform.pojo.TableInfo;
import com.siheng.metadataplatform.utils.HttpClientUtil;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: ma.shuai
 * @CreateTime: 2025-02-24  15:28
 * @Description: 告警信息
 */
@RequestMapping(value = "alert")
@RestController
public class AlertController {
    @GetMapping (value = "/telephone")
    public ResultDto telephoneAlert() throws Exception {
        String baseUrl = "https://jumfixed.market.alicloudapi.com/voice-notify/send";
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        headers.put("Authorization", "APPCODE e963017275ec4594a22af0d0cc2440b8");
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        params.put("mobile","18717888067");
        params.put("templateId","JMPQD3QERZZA");
        params.put("param","大数据,定时任务");
        return ResultDto.success(HttpClientUtil.post(baseUrl, params, headers));
    }
}
