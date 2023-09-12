package com.siheng.metadataplatform.utils;

import cn.hutool.core.util.StrUtil;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;

import java.util.*;

/**
 * @author Dearest
 * @date 2023/9/12 9:52 上午
 * @Desc
 */
public class SqlLineageUtil {

    public static Map<String, Set<String>> sqlParser(String sqlStr) {
        List<String> sqlList = StrUtil.split(sqlStr, ";");

        Map<String, Set<String>> map = new HashMap<>();
        for (String sql : sqlList) {
            if (StrUtil.isBlank(sql)) {
                continue;
            }
            MySqlStatementParser parser = new MySqlStatementParser(sql);
            SQLStatement sqlStatement = parser.parseStatement();
            MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
            sqlStatement.accept(visitor);
            Map<TableStat.Name, TableStat> tableStatMap = visitor.getTables();
            for (Map.Entry<TableStat.Name, TableStat> tableStatEntry : tableStatMap.entrySet()) {
                String name = tableStatEntry.getKey().getName();
                String value = tableStatEntry.getValue().toString();
                if (map.containsKey(value)) {
                    map.get(value).add(name);
                } else {
                    Set<String> list = new HashSet<>();
                    list.add(name);
                    map.put(value, list);
                }
            }
        }
        return map;
    }
}
