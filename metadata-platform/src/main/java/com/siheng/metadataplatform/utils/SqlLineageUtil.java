package com.siheng.metadataplatform.utils;

import cn.hutool.core.util.StrUtil;
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
            try {
                MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
                new MySqlStatementParser(sql).parseStatement().accept(visitor);
                visitor.getTables().forEach(((name, tableStat) -> {
                    if (map.containsKey(tableStat.toString())) {
                        map.get(tableStat.toString()).add(name.getName());
                    } else {
                        Set<String> list = new HashSet<>();
                        list.add(name.getName());
                        map.put(tableStat.toString(), list);
                    }
                }));
            } catch (Exception e) {
                System.out.println("解析异常:" + sqlStr);
                System.err.println(e);
            }
        }
        return map;
    }
}
