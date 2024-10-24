package com.siheng.metadataplatform.utils;

import cn.hutool.core.util.StrUtil;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import static com.siheng.metadataplatform.constant.CustomConstant.emptyStr;

/**
 * @author Dearest
 * @date 2023/9/12 9:52 上午
 * @Desc
 */
public class SqlLineageUtil {
    public static LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

    public static Map<String, Set<String>> sqlParser(String sqlStr) {
        Map<String, Set<String>> map = new HashMap<>();
        if (StrUtil.isBlank(sqlStr)) return map;
        try {
            MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
            new MySqlStatementParser(sqlStr).parseStatement().accept(visitor);
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
        return map;
    }
    public static String getInsertSet(String allContent, Set<String> select) {
        Set<String> insert = new HashSet<>();
        sqlParser(StringUtil.getParseString(allContent)).forEach((key, set) -> {
            set.forEach(e -> {
                String tableName = StringUtils.substringAfterLast(e, ".");
                if (key.startsWith("Select")) select.add(tableName);
                else if (key.startsWith("Insert")) insert.add(tableName);
            });
        });
        return insert.size() > 0 ? insert.iterator().next() : emptyStr;
    }
}
