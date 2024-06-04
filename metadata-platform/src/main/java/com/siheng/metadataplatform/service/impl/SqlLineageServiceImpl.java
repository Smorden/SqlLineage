package com.siheng.metadataplatform.service.impl;

import com.siheng.metadataplatform.mapper.neo4j.TblMapper;
import com.siheng.metadataplatform.pojo.TblRelationShip;
import com.siheng.metadataplatform.service.SqlLineageService;
import com.siheng.metadataplatform.utils.GitUtil;
import com.siheng.metadataplatform.utils.SqlLineageUtil;
import com.siheng.metadataplatform.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Dearest
 * @date 2023/9/7 10:11 上午
 * @Desc
 */
@Service
@Slf4j
public class SqlLineageServiceImpl implements SqlLineageService {

    @Resource
    private TblMapper tblMapper;


    @Override
    public void updateSqlLineage(Map<String, String> map) throws Exception {
        String start = map.getOrDefault("start", LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.THURSDAY)).toString());
        String end = map.getOrDefault("end", "2099-12-31");
        String branch = map.getOrDefault("branch", "release");
        GitUtil.getUpdateDiff(start, end, branch).forEach((code, diffs) -> {
            //diffs是每一次merge
            diffs.forEach(diff -> {//diff是每个变化的文件
                String allContent = GitUtil.getGitFileContent(diff.getNewPath(), branch);
                if (ObjectUtils.anyNull(allContent)) return;
                Set<String> select = new HashSet<>();
                String tableName = getInsertSet(allContent, select);
                if (ObjectUtils.anyNull(tableName)) return;
                if (StringUtils.contains(allContent, "@下线")) {
                    System.out.println("tableName:"+tableName);
                    tblMapper.deleteTblAndAllTblRelationShipList(tableName);
                    return;
                }
                //下面就是先删关系后增
                tblMapper.deleteBeforeAllTblRelationShipList(tableName);
                insertTblAndRelation(select, tableName);
            });
        });
    }

    @Override
    public void initDsAllSqlLineage(String branch) throws Exception {

    }

    @Override
    public void initAllSqlLineage(String branch) throws Exception {
        //删除neo4j里面所有节点和关系
        tblMapper.deleteAllTblAndAllTblRelationShip();
        //从git上获取所有的文件和文件内容
        GitUtil.getAllFilePath(branch).forEach(filePath -> {
            String allContent = GitUtil.getGitFileContent(filePath, branch);
            if (ObjectUtils.anyNull(allContent) || StringUtils.contains(allContent, "@下线"))
                return;
            System.out.println("正在执行的文件是" + filePath);
            Set<String> select = new HashSet<>();
            String tableName = getInsertSet(allContent, select);
            insertTblAndRelation(select, tableName);
        });
    }

    private void insertTblAndRelation(Set<String> select, String tableName) {
        Set<String> allTbl = new HashSet<>();
        if (StringUtils.isNotBlank(tableName) && select.size() > 0) {
            allTbl.addAll(select);
            allTbl.add(tableName);
            tblMapper.insertTblList(allTbl);
            TblRelationShip tblRelationShip = new TblRelationShip();
            tblRelationShip.setSourceTbls(select);
            tblRelationShip.setAllTbls(allTbl);
            tblRelationShip.setTargetTbl(tableName);
            tblMapper.insertTblRelationShipList(tblRelationShip);
        }
    }

    private static String getInsertSet(String allContent, Set<String> select) {
        Set<String> insert = new HashSet<>();
        SqlLineageUtil.sqlParser(StringUtil.getParseString(allContent)).forEach((key, set) -> {
            set.forEach(e -> {
                String tableName = StringUtils.substringAfterLast(e, ".");
                if (key.startsWith("Select")) select.add(tableName);
                else if (key.startsWith("Insert")) insert.add(tableName);
            });
        });
        return insert.size() > 0 ? insert.iterator().next() : "";
    }
}
