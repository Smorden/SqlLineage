package com.siheng.metadataplatform.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.siheng.metadataplatform.mapper.neo4j.TblMapper;
import com.siheng.metadataplatform.pojo.TblRelationShip;
import com.siheng.metadataplatform.service.SqlLineageService;
import com.siheng.metadataplatform.utils.GitUtil;
import com.siheng.metadataplatform.utils.SqlLineageUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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

    @Resource
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
//            System.out.println("当前的文件为-----" + filePath);
            int startIndex = allContent.indexOf("-- begin_insert --");
            String sqlContent = allContent.substring(startIndex + "-- begin_insert --".length());
            String replace = sqlContent.replace("[ broadcast ]", "")
                    .replace(";", "")
                    .replaceAll("with.*label.*@label", "")
                    .replaceAll("WITH.*label.*@label", "");
//            System.out.println(sqlContent);

            Map<String, Set<String>> stringSetMap = SqlLineageUtil.sqlParser(replace);

            Set<String> select = new HashSet<>();
            Set<String> insert = new HashSet<>();
            for (Map.Entry<String, Set<String>> entry : stringSetMap.entrySet()) {
                if (entry.getKey().equals("Select")) {
                    for (String s : entry.getValue()) {
                        int index = s.indexOf('.');
                        if (index != -1) {
                            String value = s.substring(index + 1, s.length());
                            select.add(value);

                        }

                    }
                }
                if (entry.getKey().equals("Insert")) {
                    for (String s : entry.getValue()) {
                        int index = s.indexOf('.');
                        if (index != -1) {
                            String value = s.substring(index + 1, s.length());
                            insert.add(value);
                        }

                    }
                }


            }
//            System.out.println("当前" + filePath + "    " + insert);
//            System.out.println("当前" + filePath + "    "  +  select);
//                System.out.println(stringSetMap);
            Set<String> allTbl = new HashSet<>();

            if (insert.size() > 0 && select.size() > 0) {
                allTbl.addAll(select);
                allTbl.addAll(insert);
                tblMapper.insertTblList(allTbl);

                TblRelationShip tblRelationShip = new TblRelationShip();
                tblRelationShip.setSourceTbls(select);
                tblRelationShip.setAllTbls(allTbl);
                tblRelationShip.setTargetTbl(insert.iterator().next());
                tblMapper.insertTblRelationShipList(tblRelationShip);
            }

//
//            Set<String> allTbl = new HashSet<>();
//            allTbl.addAll(select);
//            allTbl.addAll(insert);
//            tblMapper.insertTblList(allTbl);


//                tblMapper.insertTblList();
//                tblMapper.insertTblRelationShipList();


        }


    }
}
