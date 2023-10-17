package com.siheng.metadataplatform.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.siheng.metadataplatform.mapper.neo4j.TblMapper;
import com.siheng.metadataplatform.service.SqlLineageService;
import com.siheng.metadataplatform.utils.GitUtil;
import com.siheng.metadataplatform.utils.SqlLineageUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private TblMapper tblMapper;


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

    @Override
    public void initAllSqlLineage(String branch) throws Exception {
        //删除neo4j里面所有节点和关系
        tblMapper.deleteAllTblAndAllTblRelationShip();

        //从git上获取所有的文件和文件内容
        String startDate = "2022-01-01";
        String endDate = "2030-01-01";
        Set<String> allModifyFilePath = GitUtil.getAllModifyFilePath(startDate, endDate, branch);
        allModifyFilePath.remove("com.siheng.dws/dws_ivct_iw_receive_to_shelf_stage_stock_ds.sql");
        for (String filePath : allModifyFilePath) {
            String allContent = GitUtil.getGitFileContent(filePath, branch);
            if (StringUtils.isBlank(allContent)) continue;
            System.out.println("当前的文件为-----" + filePath);
            int startIndex = allContent.indexOf("-- begin_insert --");
            String sqlContent = allContent.substring(startIndex + "-- begin_insert --".length());
            String replace = sqlContent.replace("[ broadcast ]", "");
//            System.out.println(sqlContent);
            Map<String, Set<String>> stringSetMap = SqlLineageUtil.sqlParser(replace);
            System.out.println(stringSetMap);


        }


    }
}
