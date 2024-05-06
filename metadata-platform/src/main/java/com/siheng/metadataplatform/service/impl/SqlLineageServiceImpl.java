package com.siheng.metadataplatform.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.siheng.metadataplatform.mapper.neo4j.TblMapper;
import com.siheng.metadataplatform.pojo.TblRelationShip;
import com.siheng.metadataplatform.service.SqlLineageService;
import com.siheng.metadataplatform.utils.GitUtil;
import com.siheng.metadataplatform.utils.SqlLineageUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

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
        List<String> availablePackages = Arrays.asList("com.siheng.dwd.dim", "com.siheng.dwd.fact", "com.siheng.dwd.binlog", "com.siheng.dwm", "com.siheng.dws", "com.siheng.dwt", "com.siheng.market.wms");
        //从git上获取所有的文件和文件内容
        String startDate = "2022-01-01";
        String endDate = "2030-01-01";
        //删除neo4j里面所有节点和关系
        tblMapper.deleteAllTblAndAllTblRelationShip();
        GitUtil.getAllModifyFilePath(startDate, endDate, branch).forEach(filePath -> {
            String allContent = GitUtil.getGitFileContent(filePath, branch);
            if (!availablePackages.contains(StringUtils.substringBefore(filePath, "/")) || ObjectUtils.anyNull(allContent))
                return;
            System.out.println("正在执行的文件是" + filePath);
            String replace = StringUtils.substringAfterLast(allContent, "-- begin_insert --")
                    .replace("[ broadcast ]", "")
                    .replaceAll("with.*label.*@label", "")
                    .replaceAll("WITH.*label.*@label", "");
            Set<String> select = new HashSet<>();
            Set<String> insert = new HashSet<>();
            SqlLineageUtil.sqlParser(replace).forEach((key, set) -> {
                set.forEach(e -> {
                    String tableName = StringUtils.substringAfterLast(e, ".");
                    if ("Select".equals(key)) select.add(tableName);
                    else if ("Insert".equals(key)) insert.add(tableName);
                });
            });
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
        });
    }
}
