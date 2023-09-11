package com.siheng.metadataplatform.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.siheng.metadataplatform.service.SqlLineageService;
import org.gitlab4j.api.GitLabApi;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dearest
 * @date 2023/9/7 10:11 上午
 * @Desc
 */
@Service
public class SqlLineageServiceImpl implements SqlLineageService {
    @Override
    public int updateSqlLineage(Map<Object, Object> map) {
        Set<String> addAndModifyTableDirs = new HashSet<>();

        JSONArray commits = JSON.parseArray(JSON.toJSONString(map.get("commits")));
        for (Object commitObj : commits) {
            JSONObject commit = (JSONObject) commitObj;

            JSONArray added = commit.getJSONArray("added");
            JSONArray modified = commit.getJSONArray("modified");

            List<String> addedList = added.toJavaList(String.class);
            List<String> modifiedList = modified.toJavaList(String.class);

            addAndModifyTableDirs.addAll(addedList);
            addAndModifyTableDirs.addAll(modifiedList);
        }

        return 0;
    }
}
