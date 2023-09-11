package com.siheng.metadataplatform.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.siheng.metadataplatform.pojo.TJob;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Dearest
 * @date 2023/9/8 2:23 下午
 * @Desc
 */
public class InvokeUtil {

    /**
     * 执行方法
     *
     * @param job 系统任务
     */
    public static void invokeMethod(TJob job) throws Exception {
        String executeTarget = job.getExecuteTarget();
        if (StrUtil.isBlank(executeTarget)) {
            return;
        }
        String beanName = executeTarget.substring(0, executeTarget.indexOf("."));
        if (StrUtil.isBlank(beanName)) {
            return;
        }
        String methodName = executeTarget.substring(executeTarget.indexOf(".") + 1, executeTarget.indexOf("("));
        if (StrUtil.isBlank(methodName)) {
            return;
        }
        List<Object[]> methodParams = getMethodParams(executeTarget);
        //创建调用目标实例
        Object bean = null;
        if (beanName.indexOf(".") < 0) {
            //bean名为自定义的实例名
            bean = SpringUtil.getBean(beanName);
        } else {
            //bean名带包名路径
            bean = Class.forName(beanName).newInstance();
        }

        if (methodParams != null && methodParams.size() > 0) {
            Method method = bean.getClass().getDeclaredMethod(methodName, getMethodParamsType(methodParams));
            method.invoke(bean, getMethodParamsValue(methodParams));
        } else {
            Method method = bean.getClass().getDeclaredMethod(methodName);
            method.invoke(bean);
        }
    }

    /**
     * 获取参数类型
     *
     * @param methodParams 参数相关列表
     * @return 参数类型列表
     */
    public static Class<?>[] getMethodParamsType(List<Object[]> methodParams) {
        Class<?>[] classs = new Class<?>[methodParams.size()];
        int index = 0;
        for (Object[] os : methodParams) {
            classs[index] = (Class<?>) os[1];
            index++;
        }
        return classs;
    }

    /**
     * 获取参数值
     *
     * @param methodParams 参数相关列表
     * @return 参数值列表
     */
    public static Object[] getMethodParamsValue(List<Object[]> methodParams) {
        Object[] classs = new Object[methodParams.size()];
        int index = 0;
        for (Object[] os : methodParams) {
            classs[index] = (Object) os[0];
            index++;
        }
        return classs;
    }

    /**
     * 获取method方法参数相关列表
     *
     * @param invokeTarget 目标字符串
     * @return method方法相关参数列表
     */
    public static List<Object[]> getMethodParams(String invokeTarget) {
        String methodStr = invokeTarget.substring(invokeTarget.indexOf("(") + 1, invokeTarget.indexOf(")"));
        if (StrUtil.isBlank(methodStr)) {
            return null;
        }
        String[] methodParams = methodStr.split(",");
        List<Object[]> classs = new LinkedList<>();
        for (int i = 0; i < methodParams.length; i++) {
            String str = StrUtil.isBlank(methodParams[i]) ? "" : methodParams[i].trim();
            ;
            // String字符串类型，包含'
            if (str.contains("'")) {
                classs.add(new Object[]{str.replace("'", ""), String.class});
            }
            // boolean布尔类型，等于true或者false
            else if (StrUtil.equals(str.toLowerCase(), "true") || StrUtil.equals(str.toLowerCase(), "false")) {
                classs.add(new Object[]{Boolean.valueOf(str), Boolean.class});
            }
            // long长整形，包含L
            else if (str.toUpperCase().contains("L")) {
                classs.add(new Object[]{Long.valueOf(str.toUpperCase().replace("L", "")), Long.class});
            }
            // double浮点类型，包含D
            else if (str.toUpperCase().contains("D")) {
                classs.add(new Object[]{Double.valueOf(str.toUpperCase().replace("D", "")), Double.class});
            }
            // 其他类型归类为整形
            else {
                classs.add(new Object[]{Integer.valueOf(str), Integer.class});
            }
        }
        return classs;
    }
}
