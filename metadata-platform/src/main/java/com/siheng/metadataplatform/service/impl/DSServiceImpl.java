package com.siheng.metadataplatform.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.siheng.metadataplatform.dto.GlobalParamsDto;
import com.siheng.metadataplatform.dto.TaskDefinitionDto;
import com.siheng.metadataplatform.mapper.first.TableInfoMapper;
import com.siheng.metadataplatform.service.DSService;
import com.siheng.metadataplatform.utils.GitUtil;
import com.siheng.metadataplatform.utils.HttpClientUtil;
import com.siheng.metadataplatform.utils.SqlLineageUtil;
import com.siheng.metadataplatform.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.siheng.metadataplatform.constant.CustomConstant.*;
import static com.siheng.metadataplatform.utils.GitUtil.*;

/**
 * @Author: ma.shuai
 * @CreateTime: 2024-06-05  09:47
 * @Description: DS相关操作实现
 */
@Service
@Slf4j
public class DSServiceImpl implements DSService {


    @Resource
    private TableInfoMapper tableInfoMapper;

    @Override
    public void updateProcess(String targetBranch, String mergeCommitSha) throws Exception {
        GitLabApi gitLabApi = new GitLabApi(gitlabUrl, personalAccessToken);
        //找出需要添加的依赖
        Map<String, Set<String>> addLineage = new HashMap<>();
        //找出需要删除的依赖
        Map<String, Set<String>> removeLineage = new HashMap<>();
        Map<String, String> schedulers = new HashMap<>();
        String baseUrl = pre.equalsIgnoreCase(targetBranch) ? baseUrlPre : baseUrlTest;
        Map<String, Object> params = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put(token, tokenValue);
        /** 解析merge请求中的所有文件和ds元数据做对比,找出需要添加和删除的依赖*/
        gitLabApi.getCommitsApi().getDiff(projectPath, mergeCommitSha)
                .stream().filter(diff -> packages.contains(StringUtils.substringBefore(diff.getNewPath(), lashStr)))
                .forEach(diff -> {
                    String allContent = GitUtil.getGitFileContent(diff.getNewPath(), targetBranch);
                    Set<String> select = new HashSet<>();
                    String tableName = SqlLineageUtil.getInsertSet(allContent, select);
                    if (emptyStr.equals(tableName))
                        tableName = diff.getNewPath().replaceAll(".*/", emptyStr).replaceAll(".sql", emptyStr);
                    String command = StringUtils.substringBetween(allContent, schedulerStr, carriageReturn);
                    if (StringUtils.isNoneEmpty(command)) schedulers.put(tableName, command);
                    Set<String> scheduleLineage = getScheduleLineageSet(baseUrl, tableName, headers);
                    if (diff.getDeletedFile() || ObjectUtils.anyNull(allContent) || StringUtils.contains(allContent, downLine) || StringUtils.contains(allContent, processNameStr)) {
                        removeLineage.put(tableName, scheduleLineage);
                        schedulers.put(tableName, defaultCommand);
                    } else if (diff.getNewFile()) {
                        addLineage.put(tableName, select);
                    } else {
                        Set<String> selectCopy = select.stream().collect(Collectors.toSet());
                        select.removeAll(scheduleLineage);
                        scheduleLineage.removeAll(selectCopy);
                        if (!select.isEmpty()) addLineage.put(tableName, select);
                        if (!scheduleLineage.isEmpty()) removeLineage.put(tableName, scheduleLineage);
                    }
                });

        if (!addLineage.isEmpty() || !removeLineage.isEmpty()) {
            /** 下线default定时任务和工作流任务*/
            String processIdAndCode = getProcessIdAndCode(baseUrl, defaultProcessName, headers);
            String processId = StringUtils.substringBefore(processIdAndCode, commaStr);
            String processCode = StringUtils.substringAfter(processIdAndCode, commaStr);
            params.put(name, defaultProcessName);
            params.put(releaseState, offLine);
            System.out.println("下线状态是" + JSON.parseObject(HttpClientUtil.post(baseUrl + processReleaseUrl.replace(projectReplace, projectCode).replace(codeReplace, processCode), params, headers)).getBoolean(success));
            /** 删除任务之间的关系*/
            if (!removeLineage.isEmpty()) {
                params.clear();
                remveTaskRelation(removeLineage, baseUrl, params, headers, processCode);
            }
            /** 更新节点&添加节点*/
            if (!addLineage.isEmpty()) {
                Set<String> keySet = addLineage.keySet();
                addLineage.values().stream().flatMap(sets -> sets.stream()).forEach(tableName -> {
                    if (tableName.startsWith(ods)) {
                        keySet.add(tableName);
                    } else {
                        try {
                            if (JSON.parseObject(HttpClientUtil.get(baseUrl + taskUpdateUrl.replace(projectReplace, projectCode).replace(codeReplace, StringUtil.generateUniqueNumber(tableName).toString()), headers)).getBoolean(failed)) {
                                keySet.add(tableName);
                                if (!schedulers.containsKey(tableName)) schedulers.put(tableName, defaultCommand);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                createOrUpdateTask(keySet, baseUrl, params, headers, schedulers);
                /** 添加任务之间的关系*/
                createTaskRelation(addLineage, baseUrl, params, headers, processCode);
            }
            /** 上线default任务*/
            onLineProcess(baseUrl, params, headers, processCode);
            /** 上线default定时调度*/
            onLineProcessDs(baseUrl, params, headers, processId);
        }
        /** 释放下一个任务*/
        if (SqlLineageUtil.queue.peek() == mergeCommitSha) {
            SqlLineageUtil.queue.poll();
        }
    }

    private static void onLineProcessDs(String baseUrl, Map<String, Object> params, Map<String, String> headers, String processId) throws Exception {
        params.clear();
        Boolean onLineScheduleStatus = JSON.parseObject(HttpClientUtil.post(baseUrl + onLineScheduleUrl.replace(projectReplace, projectCode).replace(idReplace, processId), params, headers)).getBoolean(success);
        System.out.println("上线调度状态是:" + onLineScheduleStatus);
    }

    private static void onLineProcess(String baseUrl, Map<String, Object> params, Map<String, String> headers, String processCode) throws Exception {
        params.clear();
        params.put(name, defaultProcessName);
        params.put(releaseState, onLine);
        System.out.println("上线状态是" + JSON.parseObject(HttpClientUtil.post(baseUrl + processReleaseUrl.replace(projectReplace, projectCode).replace(codeReplace, processCode), params, headers)).getBoolean(success));
    }

    private void createTaskRelation(Map<String, Set<String>> addLineage, String baseUrl, Map<String, Object> params, Map<String, String> headers, String processCode) {
        addLineage.forEach((targetTableName, select) ->
                select.forEach(selectTableName -> {
                    params.clear();
                    params.put(projectCodeStr, projectCode);
                    params.put(preTaskCode, StringUtil.generateUniqueNumber(selectTableName).toString());
                    params.put(postTaskCode, StringUtil.generateUniqueNumber(targetTableName).toString());
                    params.put(processDefinitionCode, processCode);
                    try {
                        System.out.println("前置任务是:" + selectTableName + "后置任务是:" + targetTableName);
                        HttpClientUtil.post(baseUrl + processTaskRelationSaveUrl.replace(projectReplace, projectCode), params, headers);
                        Thread.sleep(200);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
        );
    }

    private void createOrUpdateTask(Set<String> selectSet, String baseUrl, Map<String, Object> params, Map<String, String> headers, Map<String, String> schedulers) throws Exception {

        String tasksJsonString = JSON.toJSONString(selectSet.stream()
                .map(tableName -> new TaskDefinitionDto(StringUtil.generateUniqueNumber(tableName), tableName, getSchedulerStr(schedulers, tableName)))
                .filter(task -> {
                    params.put(taskDefinitionJsonObj, JSON.toJSONString(task));
                    try {
                        Boolean isExist = JSON.parseObject(HttpClientUtil.put(baseUrl + taskUpdateUrl.replace(projectReplace, projectCode).replace(codeReplace, task.getCode().toString()), params, headers)).getBoolean(failed);
                        log.info("任务名:{},在ds中的存在状态是{}", task.getName(), isExist);
                        System.out.println("任务名" + task.getName() + ",在ds中的存在状态是" + isExist);
                        return isExist;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList()));
        params.put(taskDefinitionJson, tasksJsonString);
        log.info("保存任务字符串是:{}", tasksJsonString);
        System.out.println("保存任务字符串是:" + tasksJsonString);
        HttpClientUtil.post(baseUrl + taskSaveUrl.replace(projectReplace, projectCode), params, headers);
    }

    private void remveTaskRelation(Map<String, Set<String>> removeLineage, String baseUrl, Map<String, Object> params, Map<String, String> headers, String processCode) {
        removeLineage.forEach((targetTableName, select) ->
                select.forEach(selectTableName -> {
                    try {
                        HttpClientUtil.delete(baseUrl + deleteEdgeUrl.replace(projectReplace, projectCode)
                                .replace(preTaskCodeReplace, StringUtil.generateUniqueNumber(selectTableName).toString())
                                .replace(postTaskCodeReplace, StringUtil.generateUniqueNumber(targetTableName).toString())
                                .replace(processDefinitionCodeReplace, processCode), params, headers);
                        Thread.sleep(200);
                    } catch (Exception e) {
                        log.error("删除任务关系异常,地址是:{},preTaskCode:{},postTask:{},processDefinitionCode:{}"
                                , deleteEdgeUrl
                                , StringUtil.generateUniqueNumber(selectTableName).toString()
                                , StringUtil.generateUniqueNumber(targetTableName).toString()
                                , processCode
                        );
                        throw new RuntimeException(e);
                    }
                }));
    }

    /**
     * @description:根据表名获取上游血缘列表
     * @author: ma.shuai
     * @date: 2024/9/4 17:34
     * @param: [taskName, headers]
     * @return: java.util.Set<java.lang.String>
     **/
    public Set<String> getScheduleLineageSet(String baseUrl, String taskName, Map<String, String> headers) {
        Set<String> scheduleLineage = new HashSet<>();
        try {
            scheduleLineage = JSON.parseObject(HttpClientUtil.get(baseUrl + queryUpstreamRelationUrl.replace(projectReplace, projectCode).replace(taskCodeReplace, StringUtil.generateUniqueNumber(taskName).toString()), headers)).getJSONArray(data).stream().map(e -> {
                JSONObject jsonObject = (JSONObject) e;
                return jsonObject.getString(name);
            }).collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return scheduleLineage;
    }

    /**
     * @description:获取默认工作流code
     * @author: ma.shuai
     * @date: 2024/9/4 17:34
     * @param: [processName, headers]
     * @return: java.lang.String
     **/
    public String getProcessIdAndCode(String baseUrl, String processName, Map<String, String> headers) throws Exception {
        AtomicReference<String> processCode = new AtomicReference<>(emptyStr);
        JSONArray jsonArray = JSON.parseObject(HttpClientUtil.get(baseUrl + querySimpleListUrl.replace(projectReplace, projectCode), headers)).getJSONArray(data);
        jsonArray.stream().forEach(e -> {
            JSONObject jsonObject = (JSONObject) e;
            if (processName.equals(jsonObject.getString(name))) {
                processCode.set(jsonObject.getString(id) + commaStr + jsonObject.getString(code));
                System.out.println(jsonObject.getString(name));
                return;
            }
        });
        return processCode.get();
    }

    @Override
    public void initProcess(String branch) throws Exception {
        String baseUrl = pre.equalsIgnoreCase(branch) ? baseUrlPre : baseUrlTest;
        Map<String, Object> params = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        String processCode;
        headers.put(token, tokenValue);
        /** 1.删除默认工作流 */
        params.put(name, defaultProcessName);
        if (!JSON.parseObject(HttpClientUtil.get(baseUrl + verifyNameUrl.replace(projectReplace, projectCode), params, headers)).getBoolean(success)) {
            String deleteProcessJson = HttpClientUtil.delete(baseUrlTest + deleteByCodeUrl.replace(projectReplace, projectCode).replace(codeReplace, JSON.parseObject(HttpClientUtil.get(baseUrl + queryByNameUrl.replace(projectReplace, projectCode), params, headers)).getJSONObject(data).getJSONObject(processDefinition).getString(code)), params, headers);
            System.out.println("删除默认工作流的状态为:" + deleteProcessJson);
        }
        /** 2.创建默认工作流 */
        params.put(tenantCode, tenantCodeValue);
        params.put(description, descriptionValue);
        params.put(globalParams, getGlobalParamsJsonString());
        processCode = JSON.parseObject(HttpClientUtil.post(baseUrl + createProcessUrl.replace(projectReplace, projectCode), params, headers)).getJSONObject(data).getString(code);
        System.out.println("新创建的默认工作流code为:" + processCode);
        /** 3.查询所有血缘 */
        Map<String, Set<String>> allLineage = new HashMap<>();
        Map<String, String> schedulers = new HashMap<>();
        GitUtil.getAllFilePath(branch).forEach(filePath -> {
            String allContent = GitUtil.getGitFileContent(filePath, branch);
            Set<String> select = new HashSet<>();
            String tableName = SqlLineageUtil.getInsertSet(allContent, select);
            if (StringUtils.contains(allContent, downLine)) schedulers.put(tableName, defaultCommand);
            //  TODO 支持多工作流,目前先过滤
            if (ObjectUtils.anyNull(allContent) || StringUtils.contains(allContent, downLine) || StringUtils.contains(allContent, processNameStr)) {
                return;
            }
            String command = StringUtils.substringBetween(allContent, schedulerStr, carriageReturn);
            String add_before_task = StringUtils.substringBetween(allContent, add_before_taskStr, carriageReturn);
            if (StringUtils.isNotBlank(add_before_task))
                Arrays.stream(add_before_task.replaceAll("\\s*", emptyStr).split(commaStr)).forEach(e -> select.add(e));
            String del_before_task = StringUtils.substringBetween(allContent, del_before_taskStr, carriageReturn);
            if (StringUtils.isNotBlank(del_before_task))
                Arrays.stream(del_before_task.replaceAll("\\s*", emptyStr).split(commaStr)).forEach(e -> select.remove(e));
            if (StringUtils.isNoneEmpty(command)) schedulers.put(tableName, command);
            allLineage.put(tableName, select);
        });
        /** 4.查询配置表任务*/
        Map<String, Set<String>> taskNameLineage = tableInfoMapper.queryAll().stream()
                .filter(v -> {
                    String config;
                    if (StringUtils.isBlank(v.getConfig())) config = "{}";
                    else config = v.getConfig();
                    JSONObject configJson = JSON.parseObject(config);
                    if (ObjectUtils.allNotNull(configJson) && configJson.containsKey(schedule))
                        schedulers.put(configJson.getString(schedule), commandPrefix + schedule + blankStr + configJson.getString(schedule));
                    if (configJson.containsKey(processName) || v.getIsEnable() == 0)
                        schedulers.put(v.getTaskName(), defaultCommand);
                    //  TODO 支持多工作流,目前先过滤
                    return v.getIsEnable() == 1 && (configJson.containsKey(schedule) || configJson.containsKey(beforeTask)) && !configJson.containsKey(processName);
                })
                .collect(Collectors.toMap(key -> key.getTaskName(), v -> {
                            HashSet selectSet = new HashSet<String>();
                            String scheduleStr = JSON.parseObject(v.getConfig()).getString(schedule);
                            if (StringUtils.isNotBlank(scheduleStr)) selectSet.add(scheduleStr);
                            String beforeTaskStr = JSON.parseObject(v.getConfig()).getString(beforeTask);
                            if (StringUtils.isNotBlank(beforeTaskStr))
                                Arrays.stream(beforeTaskStr.split(commaStr)).forEach(e -> selectSet.add(e));
                            return selectSet;
                        }
                ));
        /** 5.删除任务间的关系 */
        params.clear();
        allLineage.putAll(taskNameLineage);//添加配置级别的血缘
        remveTaskRelation(allLineage, baseUrl, params, headers, processCode);
        /** 6.更新任务&创建任务 */
        Set<String> selectSet = allLineage.values().stream().flatMap(sets -> sets.stream()).collect(Collectors.toSet());
        selectSet.addAll(allLineage.keySet());
        createOrUpdateTask(selectSet, baseUrl, params, headers, schedulers);
//        Assert.isTrue(JSON.parseObject(taskSaveResult).getBoolean(success), "生成任务报错!!!");
        /** 7.创建任务间的关系和工作流关系创建 */
        createTaskRelation(allLineage, baseUrl, params, headers, processCode);
        /** 8.上线工作流 */
        onLineProcess(baseUrl, params, headers, processCode);
        /** 9.创建调度时间 */
        params.clear();
        params.put(processDefinitionCode, processCode);
        params.put(environmentCode, -1);
        params.put(failureStrategy, continue_Strategy);
        params.put(processInstancePriority, highest);
        params.put(schedule, scheduleDS);
        params.put(warningGroupId, 2);
        params.put(warningType, failure);
        params.put(workerGroup, defaultProcessName);
        params.put(workerGroupId, 2);
        String dsId = JSON.parseObject(HttpClientUtil.post(baseUrl + createScheduleUrl.replace(projectReplace, projectCode), params, headers)).getJSONObject(data).getString(id);
        /** 10.上线调度时间 */
        onLineProcessDs(baseUrl, params, headers, dsId);
    }

    private String getSchedulerStr(Map<String, String> schedulers, String tableName) {
        String schedulerStr = commandPrefix + tableName + commandSuffix;
        if (tableName.startsWith(ods)) {
            switch (tableName) {
                case "ods_ftp_amazon_report_asin_di":
                    schedulerStr = defaultCommand;
                    break;
                default:
                    schedulerStr = commandPrefix + tableName + commandSuffix + commandInit;
            }
            if (getTodayAndBeforeList().contains(tableName)) {
                schedulerStr += commandAnd + commandPrefix + tableName + commandToday;
            }
        }
        return schedulers.getOrDefault(tableName, schedulerStr);
    }

    private List<String> getTodayAndBeforeList() {
        return Arrays.asList(
                "ods_lh_disc_ic_source_coming_stock_balance_di",
                "ods_lh_disc_ic_source_flow_di",
                "ods_lh_disc_ic_source_stock_balance_di",
                "ods_odoo_erp_new_stock_year_stock2_di",
                "ods_pbbs_api_amzad_cp2_ad_report_di",
                "ods_pbbs_oc_oms_orders_detail_di",
                "ods_pbbs_oc_oms_orders_di",
                "ods_pbbs_oc_oms_orders_lineitem_di",
                "ods_lh_bp_bg_sku_calculation_result_di",
                "ods_pbbs_oc_obc_order_theory_common_detail_di",
                "ods_pbbs_oc_obc_order_theory_cost_detail_di",
                "ods_pbbs_oc_obc_order_theory_fee_di",
                "ods_pbbs_oc_obc_order_theory_other_fee_di",
                "ods_pbbs_oc_obc_order_theory_storage_fee_detail_di",
                "ods_pbbs_oc_obc_order_theory_trans_fee_detail_di",
                "ods_pbbs_oc_obc_order_theory_trans_fee_info_di");
    }

    private String getGlobalParamsJsonString() {
        return JSON.toJSONString(Arrays.asList(
                new GlobalParamsDto("before_1", "$[yyyy-MM-dd-1]"),
                new GlobalParamsDto("before_2", "$[yyyy-MM-dd-2]"),
                new GlobalParamsDto("before_3", "$[yyyy-MM-dd-3]"),
                new GlobalParamsDto("before_7", "$[yyyy-MM-dd-7]"),
                new GlobalParamsDto("before_30", "$[yyyy-MM-dd-30]"),
                new GlobalParamsDto("before_45", "$[yyyy-MM-dd-45]"),
                new GlobalParamsDto("before_60", "$[yyyy-MM-dd-60]"),
                new GlobalParamsDto("before_90", "$[yyyy-MM-dd-90]"),
                new GlobalParamsDto("td", "$[yyyy-MM-dd]"),
                new GlobalParamsDto("init_date", "2022-01-01"),
                new GlobalParamsDto("after_1", "$[yyyy-MM-dd+1]"),
                new GlobalParamsDto("after_120", "$[yyyy-MM-dd+120]"),
                new GlobalParamsDto("after_250", "$[yyyy-MM-dd+250]"),
                new GlobalParamsDto("after_365", "$[yyyy-MM-dd+365]"),
                new GlobalParamsDto("after_400", "$[yyyy-MM-dd+400]"))
        );
    }
}
