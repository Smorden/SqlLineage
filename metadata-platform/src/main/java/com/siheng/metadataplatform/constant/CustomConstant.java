package com.siheng.metadataplatform.constant;

/**
 * @Author: ma.shuai
 * @CreateTime: 2024-06-13  17:00
 * @Description: 自定义常量
 */
public class CustomConstant {

    public static String projectCode = "9328171016096";
    public static String projectCodeStr = "projectCode";
    public static String projectReplace = "{projectCode}";
    public static String codeReplace = "{code}";
    public static String taskCodeReplace = "{taskCode}";
    public static String postTaskCodeReplace = "{postTaskCode}";
    public static String preTaskCodeReplace = "{preTaskCode}";
    public static String processDefinitionCodeReplace = "{processDefinitionCode}";
    public static String defaultProcessName = "default";
    public static String releaseState = "releaseState";
    public static String onLine = "ONLINE";
    public static String offLine = "OFFLINE";

    public static String branch = "branch";
    public static String test = "test";
    public static String pre = "pre";
    public static String release = "release";
    public static String code = "code";
    public static String processDefinition = "processDefinition";
    public static String taskDefinitionJsonObj = "taskDefinitionJsonObj";

    public static String data = "data";
    public static String taskCode = "taskCode";
    public static String postTaskCodes = "postTaskCodes";
    public static String postTaskCode = "postTaskCode";
    public static String preTaskCode = "preTaskCode";
    public static String processDefinitionCode = "processDefinitionCode";
    public static String environmentCode = "environmentCode";
    public static String failureStrategy = "failureStrategy";
    public static String processInstancePriority = "processInstancePriority";
    public static String highest = "HIGHEST";
    public static String continue_Strategy = "CONTINUE";
    public static String warningGroupId = "warningGroupId";
    public static String warningType = "warningType";
    public static String failure = "FAILURE";
    public static String workerGroup = "workerGroup";
    public static String workerGroupId = "workerGroupId";
    public static String baseUrlTest = "http://fe1.service-test.oigbuy.com:12345";
    public static String baseUrlPre = "http://ds3.service-pre.oigbuy.com:12345";
//    public static String baseUrlOnline = "http://ds.service-online.oigbuy.com:12345";

    public static String name = "name";
    public static String id = "id";
    public static String idReplace = "{id}";

    public static String description = "description";
    public static String taskDefinitionJson = "taskDefinitionJson";
    public static String descriptionValue = "默认工作流";
    public static String tenantCode = "tenantCode";
    public static String tenantCodeValue = "admin";
    public static String globalParams = "globalParams";
    public static String token = "Token";
    public static String success = "success";
    public static String failed = "failed";
    public static String ods = "ods";
    public static String ftpCommandPrefix = "ftpfile ";
    public static String commandPrefix = "doris ";
    public static String commandSuffix = " ${before_1}";
    public static String commandAnd = " && ";
    public static String commandToday = " ${td}";
    public static String commandInit = " ${init_date}";
    public static String defaultCommand = "echo 111";
    public static String downLine = "@下线";
    public static String schedulerStr = "-- scheduler:";
    public static String processNameStr = "-- process_name:";
    public static String add_before_taskStr = "-- add_before_task:";
    public static String del_before_taskStr = "-- del_before_task:";
    public static String processName = "process_name";
    public static String carriageReturn = "\n";
    public static String lashStr = "/";
    public static String schedule = "schedule";
    public static String scheduleDS = "{\"startTime\":\"2019-06-10 00:00:00\",\"endTime\":\"2099-06-13 00:00:00\",\"timezoneId\":\"Asia/Shanghai\",\"crontab\":\"0 30 0 * * ? *\"}";
    public static String blankStr = " ";
    public static String emptyStr = "";
    public static String commaStr = ",";
    public static String beforeTask = "before_task";
    public static String tokenValue = "7e7a6b27bbba0277de0abc5540f761f6";
    public static String verifyNameUrl = "/dolphinscheduler/projects/{projectCode}/process-definition/verify-name";
    public static String queryByNameUrl = "/dolphinscheduler/projects/{projectCode}/process-definition/query-by-name";
    public static String deleteByCodeUrl = "/dolphinscheduler/projects/{projectCode}/process-definition/{code}";
    public static String createProcessUrl = "/dolphinscheduler/projects/{projectCode}/process-definition/empty";
    public static String taskSaveUrl = "/dolphinscheduler/projects/{projectCode}/task-definition";
    public static String taskUpdateUrl = "/dolphinscheduler/projects/{projectCode}/task-definition/{code}";
    public static String deleteDownstreamRelationUrl = "/dolphinscheduler/projects/{projectCode}/process-task-relation/{code}/downstream";
    public static String deleteEdgeUrl = "/dolphinscheduler/projects/{projectCode}/process-task-relation/{processDefinitionCode}/{preTaskCode}/{postTaskCode}";
    public static String processTaskRelationSaveUrl = "/dolphinscheduler/projects/{projectCode}/process-task-relation";
    public static String processReleaseUrl = "/dolphinscheduler/projects/{projectCode}/process-definition/{code}/release";
    public static String createScheduleUrl = "/dolphinscheduler/projects/{projectCode}/schedules";
    public static String updateScheduleUrl = "/dolphinscheduler/projects/{projectCode}/process-definition/{code}/release";
    public static String onLineScheduleUrl = "/dolphinscheduler/projects/{projectCode}/schedules/{id}/online";
    public static String queryUpstreamRelationUrl = "/dolphinscheduler/projects/{projectCode}/process-task-relation/{taskCode}/upstream";
    public static String querySimpleListUrl = "/dolphinscheduler/projects/{projectCode}/process-definition/simple-list";


}
