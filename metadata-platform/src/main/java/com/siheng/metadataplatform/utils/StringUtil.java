package com.siheng.metadataplatform.utils;

import java.security.MessageDigest;

/**
 * @Author: ma.shuai
 * @CreateTime: 2024-05-13  11:21
 * @Description: 字符串工具类
 */
public class StringUtil {

    public static Long generateUniqueNumber(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            // 将字节转换成正整数
            long hashCode = 0;
            for (byte b : hash) {
                hashCode = hashCode * 31 + (b & 0xff);
                hashCode &= 0x00000000ffffffffL; // 确保是正数
            }
            return hashCode;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getParseString(String replace) {
        return org.apache.commons.lang3.StringUtils.substringAfterLast(replace, "-- begin_insert --")
                .replace("[ broadcast ]", "")
                .replaceAll(";", "")
                .replaceAll("with.*label.*@label", "")
                .replaceAll("WITH.*label.*@label", "");
    }

}
