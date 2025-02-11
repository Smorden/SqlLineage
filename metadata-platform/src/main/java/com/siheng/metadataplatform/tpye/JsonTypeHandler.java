package com.siheng.metadataplatform.tpye;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Author: ma.shuai
 * @CreateTime: 2024-12-10  10:24
 * @Description: 自定义映射json
 */
public class JsonTypeHandler implements TypeHandler<JSONObject> {
    @Override
    public void setParameter(PreparedStatement ps, int i, JSONObject parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i,parameter==null?"{}":parameter.toJSONString());
    }

    @Override
    public JSONObject getResult(ResultSet rs, String columnName) throws SQLException {
        return JSON.parseObject(rs.getString(columnName));
    }

    @Override
    public JSONObject getResult(ResultSet rs, int columnIndex) throws SQLException {
        return JSON.parseObject(rs.getString(columnIndex));
    }

    @Override
    public JSONObject getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return  JSON.parseObject(cs.getString(columnIndex));
    }
}
