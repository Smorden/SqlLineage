package com.siheng.metadataplatform.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.siheng.metadataplatform.dto.TaskDefinitionDto;
import com.siheng.metadataplatform.mapper.first.GlobalParamsMapper;
import com.siheng.metadataplatform.mapper.first.TableInfoMapper;
import com.siheng.metadataplatform.pojo.GlobalParams;
import com.siheng.metadataplatform.pojo.TableInfo;
import com.siheng.metadataplatform.service.DSService;
import com.siheng.metadataplatform.utils.GitUtil;
import com.siheng.metadataplatform.utils.HttpClientUtil;
import com.siheng.metadataplatform.utils.SqlLineageUtil;
import com.siheng.metadataplatform.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${siheng.ds.baseUrl}")
    private String baseUrl;
    @Value("${siheng.ds.token}")
    private String tokenValue;
    @Resource
    private TableInfoMapper tableInfoMapper;
    @Resource
    private GlobalParamsMapper globalParamsMapper;

    @Override
    public void updateProcess(String uniqueKey) throws Exception {

        String targetBranch = StringUtils.substringBefore(uniqueKey, commaStr);
        String mergeCommitSha = StringUtils.substringAfter(uniqueKey, commaStr);

        GitLabApi gitLabApi = new GitLabApi(gitlabUrl, personalAccessToken);
        //找出需要添加的依赖
        Map<String, Set<String>> addLineage = new HashMap<>();
        //找出需要删除的依赖
        Map<String, Set<String>> removeLineage = new HashMap<>();
        //找出需要删除的节点
        Set<String> removeTasks = new HashSet<>();
        Map<String, String> schedulers = new HashMap<>();
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
                    String scheduleTask = StringUtils.substringBetween(allContent, scheduleStr, carriageReturn);
                    if (StringUtils.isNotBlank(scheduleTask)){
                        select.add(scheduleTask);
                        schedulers.put(scheduleTask, commandPrefix + schedule + blankStr + scheduleTask);
                    }
                    String command = StringUtils.substringBetween(allContent, execStr, carriageReturn);
                    if (StringUtils.isNoneEmpty(command)) schedulers.put(tableName, command);
                    String add_before_task = StringUtils.substringBetween(allContent, add_before_taskStr, carriageReturn);
                    if (StringUtils.isNotBlank(add_before_task))
                        Arrays.stream(add_before_task.replaceAll("\\s*", emptyStr).split(commaStr)).forEach(e -> select.add(e));
                    String del_before_task = StringUtils.substringBetween(allContent, del_before_taskStr, carriageReturn);
                    if (StringUtils.isNotBlank(del_before_task))
                        Arrays.stream(del_before_task.replaceAll("\\s*", emptyStr).split(commaStr)).forEach(e -> select.remove(e));
                    Set<String> scheduleLineage = getScheduleLineageSet(baseUrl, tableName, headers);
                    if (diff.getDeletedFile() || ObjectUtils.anyNull(allContent) || StringUtils.contains(allContent, downLine) || StringUtils.contains(allContent, processNameStr)) {
                        removeLineage.put(tableName, scheduleLineage);
                        schedulers.put(tableName, defaultCommand);
                        removeTasks.add(tableName);
                    } else if (diff.getNewFile()) {
                        addLineage.put(tableName, select);
                    } else {
                        Set<String> selectCopy = select.stream().collect(Collectors.toSet());
                        select.removeAll(scheduleLineage);
                        scheduleLineage.removeAll(selectCopy);
                        addLineage.put(tableName, select);
                        if (!scheduleLineage.isEmpty()) removeLineage.put(tableName, scheduleLineage);
                    }
                });
        if (!addLineage.isEmpty() || !removeLineage.isEmpty()) {
            /** 查询tableInfo里面的默认命令*/
            tableInfoMapper.queryAll().stream().forEach(t -> {
                if (t.getConfig().containsKey(exec)) schedulers.put(t.getTaskName(), t.config.getString(exec));
            });
            updateDs(addLineage, removeLineage, removeTasks, schedulers, baseUrl);
        }
        /** 释放下一个任务*/
        if (SqlLineageUtil.queue.peek() == uniqueKey) {
            SqlLineageUtil.queue.poll();
        }
    }

    private void updateDs(Map<String, Set<String>> addLineage, Map<String, Set<String>> removeLineage, Set<String> removeTasks, Map<String, String> schedulers, String baseUrl) throws Exception {
        Map<String, Object> params = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put(token, tokenValue);
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
            //删除节点removeLineage的keys
            removeTask(removeTasks, baseUrl, params, headers, processCode);
        }
        /** 更新节点&添加节点*/
        if (!addLineage.isEmpty()) {
            Set<String> keySet = addLineage.keySet().stream().collect(Collectors.toSet());
            addLineage.values().stream().flatMap(sets -> sets.stream()).forEach(tableName -> {
                if (tableName.startsWith(ods)) {
                    keySet.add(tableName);
                } else {
                    try {
                        if (JSON.parseObject(HttpClientUtil.get(baseUrl + taskUpdateUrl.replace(projectReplace, projectCode).replace(codeReplace, StringUtil.generateUniqueNumber(tableName).toString()), headers)).getBoolean(failed)) {
                            System.out.println("tableName----->" + tableName);
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
        onLineProcessDsAndQueryId(baseUrl, params, headers, processId, processCode);
    }

    private void removeTask(Set<String> keySet, String baseUrl, Map<String, Object> params, Map<String, String> headers, String processCode) {
        params.clear();
        params.put(processDefinitionCode, processCode);
        keySet.stream().forEach(taskName -> {
            try {
                System.out.println("即将删除的节点是:" + taskName);
                HttpClientUtil.delete(baseUrl + deleteRelationUrl.replace(projectReplace, projectCode).replace(taskCodeReplace, StringUtil.generateUniqueNumber(taskName).toString()), params, headers);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void onLineProcessDsAndQueryId(String baseUrl, Map<String, Object> params, Map<String, String> headers, String processId, String processCode) throws Exception {
        params.clear();
        params.put(processDefinitionCode, processCode);
        params.put(processDefinitionId, processId);
        params.put(pageNo, 1);
        params.put(pageSize, 1);
        Boolean onLineScheduleStatus = JSON.parseObject(HttpClientUtil.post(baseUrl + onLineScheduleUrl.replace(projectReplace, projectCode).replace(idReplace, JSON.parseObject(HttpClientUtil.get(baseUrl + queryScheduleListPagingUrl.replace(projectReplace, projectCode), params, headers)).getJSONObject(data).getJSONArray(totalList).getJSONObject(0).getString(id)), params, headers)).getBoolean(success);
        System.out.println("上线调度状态是:" + onLineScheduleStatus);
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
                    int again_count = 3;
                    int process_count = 0;
                    while (again_count > 0) {
                        try {
                            System.out.println("前置任务是:" + selectTableName + "后置任务是:" + targetTableName);
                            String post = HttpClientUtil.post(baseUrl + processTaskRelationSaveUrl.replace(projectReplace, projectCode), params, headers);
                            if (JSON.parseObject(post).getBoolean(failed))
                                System.out.println("任务错误!!! response-->" + post);
                            again_count = 0;
                        } catch (Exception e) {
                            again_count--;
                            process_count++;
                            System.out.println("前置任务是:" + selectTableName + "后置任务是:" + targetTableName + "---->异常!!! 重复执行第" + process_count + "次执行");
                            if (again_count < 1) throw new RuntimeException(e);
                        }
                    }
                })
        );
    }

    private void createOrUpdateTask(Set<String> selectSet, String baseUrl, Map<String, Object> params, Map<String, String> headers, Map<String, String> schedulers) throws Exception {
        List<TaskDefinitionDto> addTaskList = selectSet.stream()
                .map(tableName -> new TaskDefinitionDto(StringUtil.generateUniqueNumber(tableName), tableName, getSchedulerStr(schedulers, tableName)))
                .filter(task -> {
                    params.put(taskDefinitionJsonObj, JSON.toJSONString(task));
                    try {
                        String response = HttpClientUtil.put(baseUrl + taskUpdateUrl.replace(projectReplace, projectCode).replace(codeReplace, task.getCode().toString()), params, headers);
                        Boolean isExist = JSON.parseObject(response).getBoolean(failed);
                        if (isExist)
                            System.out.println("任务名:" + task.getName() + ",在ds中的存在状态是:" + !isExist);
                        Thread.sleep(200);
                        return isExist;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        if (!addTaskList.isEmpty()) {
            String tasksJsonString = JSON.toJSONString(addTaskList);
            params.put(taskDefinitionJson, tasksJsonString);
            log.info("保存任务字符串是:{}", tasksJsonString);
            System.out.println("保存任务字符串是:" + tasksJsonString);
            HttpClientUtil.post(baseUrl + taskSaveUrl.replace(projectReplace, projectCode), params, headers);
        }
    }

    private void remveTaskRelation(Map<String, Set<String>> removeLineage, String baseUrl, Map<String, Object> params, Map<String, String> headers, String processCode) {
        removeLineage.forEach((targetTableName, select) ->
                select.forEach(selectTableName -> {
                    try {
                        String delete = HttpClientUtil.delete(baseUrl + deleteEdgeUrl.replace(projectReplace, projectCode)
                                .replace(preTaskCodeReplace, StringUtil.generateUniqueNumber(selectTableName).toString())
                                .replace(postTaskCodeReplace, StringUtil.generateUniqueNumber(targetTableName).toString())
                                .replace(processDefinitionCodeReplace, processCode), params, headers);
                        if (JSON.parseObject(delete).getBoolean(failed))
                            System.out.println("删除关系有问题!!!!  response:" + delete);
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
                processCode.set(StringUtils.join(jsonObject.getString(id), commaStr, jsonObject.getString(code)));
                return;
            }
        });
        return processCode.get();
    }

    @Override
    public void initProcess(String branch) throws Exception {
        System.out.println("baseUrl----------------->" + baseUrl);
        Map<String, Object> params = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        String processCode;
        headers.put(token, tokenValue);
        /** 1.删除默认工作流 */
        params.put(name, defaultProcessName);
        System.out.println(HttpClientUtil.get(baseUrl + verifyNameUrl.replace(projectReplace, projectCode), params, headers));
        if (!JSON.parseObject(HttpClientUtil.get(baseUrl + verifyNameUrl.replace(projectReplace, projectCode), params, headers)).getBoolean(success)) {
            String deleteProcessJson = HttpClientUtil.delete(baseUrl + deleteByCodeUrl.replace(projectReplace, projectCode).replace(codeReplace, JSON.parseObject(HttpClientUtil.get(baseUrl + queryByNameUrl.replace(projectReplace, projectCode), params, headers)).getJSONObject(data).getJSONObject(processDefinition).getString(code)), params, headers);
            System.out.println("删除默认工作流的状态为:" + deleteProcessJson);
        }
        /** 2.创建默认工作流 */
        params.put(tenantCode, tenantCodeValue);
        params.put(description, descriptionValue);
        params.put(globalParams, JSON.toJSONString(globalParamsMapper.queryAll()));
        processCode = JSON.parseObject(HttpClientUtil.post(baseUrl + createProcessUrl.replace(projectReplace, projectCode), params, headers)).getJSONObject(data).getString(code);
        System.out.println("新创建的默认工作流code为:" + processCode);
        /** 3.查询所有血缘 */
        Map<String, Set<String>> allLineage = new HashMap<>();
        Map<String, String> schedulers = new HashMap<>();
        GitUtil.getAllFilePath(branch).forEach(filePath -> {
            String allContent = GitUtil.getGitFileContent(filePath, branch);
            Set<String> select = new HashSet<>();
            String tableName = SqlLineageUtil.getInsertSet(allContent, select);
            //  TODO 支持多工作流,目前先过滤
            if (ObjectUtils.anyNull(allContent) || StringUtils.contains(allContent, downLine) || StringUtils.contains(allContent, processNameStr)) {
                schedulers.put(tableName, defaultCommand);
                return;
            }
            String scheduleTask = StringUtils.substringBetween(allContent, scheduleStr, carriageReturn);
            if (StringUtils.isNotBlank(scheduleTask)){
                select.add(scheduleTask);
                schedulers.put(scheduleTask, commandPrefix + schedule + blankStr + scheduleTask);
            }
            String command = StringUtils.substringBetween(allContent, execStr, carriageReturn);
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
                    JSONObject configJson = v.getConfig();
                    if (configJson.containsKey(exec)) schedulers.put(v.getTaskName(), configJson.getString(exec));
                    if (ObjectUtils.allNotNull(configJson) && configJson.containsKey(schedule))
                        schedulers.put(configJson.getString(schedule), commandPrefix + schedule + blankStr + configJson.getString(schedule));
                    if (v.getIsEnable() == 0 || (null != v.getConfig().getString(processName) && !defaultProcessName.equals(v.getConfig().getString(processName))))
                        schedulers.put(v.getTaskName(), defaultCommand);
                    //  TODO 支持多工作流,目前先过滤
                    return v.getIsEnable() == 1 && (!configJson.containsKey(processName) || defaultProcessName.equals(v.getConfig().getString(processName)));
                })
                .collect(Collectors.toMap(key -> key.getTaskName(), v -> {
                            HashSet selectSet = new HashSet<String>();
                            selectSet.add(start);//配置中的任务全部依赖start任务
                            String scheduleStr = v.getConfig().getString(schedule);
                            if (StringUtils.isNotBlank(scheduleStr)) selectSet.add(scheduleStr);
                            String beforeTaskStr = v.getConfig().getString(beforeTask);
                            if (StringUtils.isNotBlank(beforeTaskStr))
                                Arrays.stream(beforeTaskStr.split(commaStr)).forEach(e -> selectSet.add(e));
                            return selectSet;
                        }
                ));
        /** 5.血缘合并 */
        params.clear();
        allLineage.putAll(taskNameLineage);//添加配置级别的血缘
//        remveTaskRelation(allLineage, baseUrl, params, headers, processCode);
        /** 6.更新任务&创建任务 */
        Set<String> selectSet = allLineage.values().stream().flatMap(sets -> sets.stream()).collect(Collectors.toSet());
        selectSet.addAll(allLineage.keySet());
        createOrUpdateTask(selectSet, baseUrl, params, headers, schedulers);
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

    @Override
    public List<GlobalParams> save(GlobalParams gp) throws Exception {
        List<GlobalParams> globalParamsAll;
        if (StringUtils.isNoneEmpty(gp.getProp(), gp.getValue())) {
            globalParamsMapper.save(gp);
//            String baseUrl = baseUrlTest;
            Map<String, Object> params = new HashMap<>();
            Map<String, String> headers = new HashMap<>();
            headers.put(token, tokenValue);
            params.put(name, defaultProcessName);
            /** 下线default定时任务和工作流任务*/
            String processIdAndCode = getProcessIdAndCode(baseUrl, defaultProcessName, headers);
            String processId = StringUtils.substringBefore(processIdAndCode, commaStr);
            String processCode = StringUtils.substringAfter(processIdAndCode, commaStr);
            params.put(releaseState, offLine);
            System.out.println("下线状态是" + JSON.parseObject(HttpClientUtil.post(baseUrl + processReleaseUrl.replace(projectReplace, projectCode).replace(codeReplace, processCode), params, headers)).getBoolean(success));
            globalParamsAll = globalParamsMapper.queryAll();
            params.put(tenantCode, tenantCodeValue);
            params.put(globalParams, JSON.toJSONString(globalParamsAll));
            HttpClientUtil.put(baseUrl + updateBasicInfoPagingUrl.replace(projectReplace, projectCode).replace(codeReplace, processCode), params, headers);
            /** 上线default任务*/
            onLineProcess(baseUrl, params, headers, processCode);
            /** 上线default定时调度*/
            onLineProcessDsAndQueryId(baseUrl, params, headers, processId, processCode);
        } else {
            globalParamsAll = globalParamsMapper.queryAll();
        }
        return globalParamsAll;
    }

    @Override
    public String startProcessInstance(Map<String, String> requestMap) throws Exception {
//        String baseUrl = baseUrlTest;
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        headers.put(token, tokenValue);
        params.put(name, requestMap.getOrDefault(processName, defaultProcessName));
        /** 下线default定时任务和工作流任务*/
        String processIdAndCode = getProcessIdAndCode(baseUrl, requestMap.getOrDefault(processName, defaultProcessName), headers);
        String processCode = StringUtils.substringAfter(processIdAndCode, commaStr);
        params.clear();
        params.put(failureStrategy, continue_Strategy);
        params.put(processInstancePriority, medium);
        params.put(projectCodeStr, projectCode);
        params.put(warningGroupId, 2);
        params.put(warningType, failure);
        params.putAll(requestMap);
        params.put(processDefinitionCode, processCode);
        params.put(startNodeList, getTaskCode(requestMap.get(taskName)));
        return HttpClientUtil.post(baseUrl + startProcessInstanceUrl.replace(projectReplace, projectCode), params, headers);
    }

    private TableInfo getDemoTableInfo() {
        return new TableInfo("填写任务名称", "服务名称", "库名称", "表名称", new JSONObject()
                .fluentPut("sink_db", "代表写入doris的库名,默认:ods")
                .fluentPut("sink_table", "代表写入doris的表名,默认:taskName")
                .fluentPut("partition_column", "指定按照某个时间字段抽取数据,抽取为分区表时使用")
                .fluentPut("exclude_column", "指定抽取过程中排除的字段用逗号分割,且需用单引号扩起来")
                .fluentPut("comment", "代表写入doris的表注释")
                .fluentPut("pre_sql", "代表导出任务的前置执行任务")
                .fluentPut("is_snapshot", "1代表需要快照(只针对全量抽取的表生效)")
                .fluentPut("split_pk", "指定表中用于分片的字段(为均匀考虑使用类似id字段)")
                .fluentPut("channel", "指定分片个数(默认为10)")
                .fluentPut("schedule", "调度时间hh:mm:ss,例如05:00:00")
                .fluentPut("befor_task", "指定前置任务")
                .fluentPut("process_name", "指定工作流名称,默认default")
                .fluentPut("app_id", "应用id(针对http发送简道云任务)")
                .fluentPut("entry_id", "表单id(针对http发送简道云任务)")
                .fluentPut("primary_key", "常规字段(针对http发送简道云任务)"), 1, 0);
    }

    @Override
    public TableInfo getTableInfoByTaskName(TableInfo tableInfo) throws Exception {
        if (ObjectUtils.anyNull(tableInfo) || StringUtils.isBlank(tableInfo.getTaskName())) {
            return getDemoTableInfo();
        }
        TableInfo queryTableInfo = tableInfoMapper.queryByTaskName(tableInfo.getTaskName());
        if (Arrays.stream(tableInfo.getClass().getDeclaredFields()).filter(f -> {
            try {
                return f.getName() != "taskName" && f.get(tableInfo) != null;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet()).isEmpty()) {
            if (ObjectUtils.anyNull(queryTableInfo)) return getDemoTableInfo();
            else return queryTableInfo;
        }
        if (null == tableInfo.getConfig()) tableInfo.setConfig(new JSONObject());
        /** 保存至数据库*/
        tableInfoMapper.save(tableInfo);

        /** 开始操作ds*/
        //找出需要添加的依赖
        Map<String, Set<String>> addLineage = new HashMap<>();
        //找出需要删除的依赖
        Map<String, Set<String>> removeLineage = new HashMap<>();
        Map<String, String> schedulers = new HashMap<>();
        //找出需要删除的节点
        Set<String> removeTasks = new HashSet<>();
        //处理ds的逻辑
        if (null == queryTableInfo) {//新增
            if ((defaultProcessName.equals(tableInfo.getConfig().getString(processName)) || null == tableInfo.getConfig().getString(processName)) && 1 == tableInfo.isEnable) {
                addLineage.put(tableInfo.getTaskName(), getSelectSet(tableInfo));
            }
        } else {//修改
            if (!StringUtils.equals(tableInfo.getConfig().getString(exec), queryTableInfo.getConfig().getString(exec))) {
                addLineage.put(tableInfo.getTaskName(), new HashSet<>());
                if (tableInfo.getConfig().containsKey(exec))
                    schedulers.put(tableInfo.getTaskName(), tableInfo.config.getString(exec));
            }
            if (queryTableInfo.isEnable == 1 && (null == queryTableInfo.getConfig().getString(processName) || defaultProcessName.equals(queryTableInfo.getConfig().getString(processName))) && (tableInfo.isEnable == 0 || (null != tableInfo.getConfig().getString(processName) && !defaultProcessName.equals(tableInfo.getConfig().getString(processName))))) {
                //删除当前节点
                removeLineage.put(queryTableInfo.getTaskName(), getSelectSet(queryTableInfo));
                removeTasks.add(queryTableInfo.getTaskName());
            } else if ((queryTableInfo.isEnable == 0 || (null != queryTableInfo.getConfig().getString(processName) && !defaultProcessName.equals(queryTableInfo.getConfig().getString(processName)))) && tableInfo.isEnable == 1 && (tableInfo.getConfig().getString(processName) == null || defaultProcessName.equals(tableInfo.getConfig().getString(processName)))) {
                //ds新增当前节点
                addLineage.put(tableInfo.getTaskName(), getSelectSet(tableInfo));
            } else if (queryTableInfo.isEnable == 1 && (null == queryTableInfo.getConfig().getString(processName) || defaultProcessName.equals(queryTableInfo.getConfig().getString(processName))) && tableInfo.isEnable == 1 && (null == tableInfo.getConfig().getString(processName) || defaultProcessName.equals(tableInfo.getConfig().getString(processName)))) {
                //修改schedule
                String querySchedule = queryTableInfo.getConfig().getString(schedule);
                String tableSchedule = tableInfo.getConfig().getString(schedule);
                if (!StringUtils.equals(querySchedule, tableSchedule)) {
                    HashSet addSelectSet = new HashSet<String>();
                    //ds新增当前节点
                    addSelectSet.add(tableSchedule);
                    addSelectSet.remove(null);
                    if (!addSelectSet.isEmpty()) addLineage.put(tableInfo.getTaskName(), addSelectSet);
                    if (null != querySchedule) {
                        HashSet removeSelectSet = new HashSet<String>();
                        removeSelectSet.add(querySchedule);
                        removeLineage.put(queryTableInfo.getTaskName(), removeSelectSet);
                    }
                }
                //修改beforeTask
                String queryBeforeTaskStr = queryTableInfo.getConfig().getString(beforeTask);
                String tableBeforeTaskStr = tableInfo.getConfig().getString(beforeTask);

                if (!StringUtils.equals(queryBeforeTaskStr, tableBeforeTaskStr)) {
                    Set<String> queryBeforeSet = new HashSet<>();
                    Set<String> tableBeforeSet = new HashSet<>();
                    if (StringUtils.isNotBlank(queryBeforeTaskStr))
                        Arrays.stream(queryBeforeTaskStr.split(commaStr)).forEach(e -> queryBeforeSet.add(e));
                    if (StringUtils.isNotBlank(tableBeforeTaskStr))
                        Arrays.stream(tableBeforeTaskStr.split(commaStr)).forEach(e -> tableBeforeSet.add(e));
                    Set<String> queryBeforeSetCopy = queryBeforeSet.stream().collect(Collectors.toSet());
                    queryBeforeSet.removeAll(tableBeforeSet);
                    tableBeforeSet.removeAll(queryBeforeSetCopy);
                    if (!queryBeforeSet.isEmpty()) removeLineage.put(tableInfo.getTaskName(), queryBeforeSet);
                    if (!tableBeforeSet.isEmpty()) addLineage.put(queryTableInfo.getTaskName(), tableBeforeSet);
                }
            }
        }
        if (!addLineage.isEmpty() || !removeLineage.isEmpty()) {
            updateDs(addLineage, removeLineage, removeTasks, schedulers, baseUrl);
        }
        return tableInfoMapper.queryByTaskName(tableInfo.getTaskName());
    }

    private static HashSet getSelectSet(TableInfo tableInfo) {
        HashSet selectSet = new HashSet<String>();
        //ds新增当前节点
        selectSet.add(start);
        //新增beforeTask和schedule
        selectSet.add(tableInfo.getConfig().getString(schedule));
        String beforeTaskStr = tableInfo.getConfig().getString(beforeTask);
        if (StringUtils.isNotBlank(beforeTaskStr))
            Arrays.stream(beforeTaskStr.split(commaStr)).forEach(e -> selectSet.add(e));
        selectSet.remove(null);
        return selectSet;
    }

    private String getTaskCode(String taskNames) {
        return Arrays.stream(StringUtils.split(taskNames, commaStr)).map(tn -> StringUtil.generateUniqueNumber(tn).toString()).collect(Collectors.joining(commaStr));
    }

    private String getDefaultCommand(String tableName) {
        return commandPrefix + tableName + commandSuffix;
    }

    private String getOdsDefaultCommand(String tableName) {
        return commandPrefix + tableName + commandSuffix + commandInit;
    }

    private String getSchedulerStr(Map<String, String> schedulers, String tableName) {
        String schedulerStr = tableName.startsWith(ods) ? getOdsDefaultCommand(tableName) : getDefaultCommand(tableName);
        if ("ods_ftp_amazon_report_asin_di".equals(tableName)) schedulerStr = defaultCommand;
        if (start.equals(tableName)) schedulerStr = startCommand;
        return schedulers.getOrDefault(tableName, schedulerStr);
    }

}
