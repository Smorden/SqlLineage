package com.siheng.metadataplatform.utils;

import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Diff;
import org.gitlab4j.api.models.MergeRequestFilter;
import org.gitlab4j.api.models.RepositoryFile;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Dearest
 * @date 2023/9/5 4:14 下午
 * @Desc
 */
public class GitUtil {

    public static String gitlabUrl = "http://git.server.oigbuy.com";
    public static String personalAccessToken = "-qnhcgHjSnzQ-uqASU7s";
    public static String projectPath = "bigdata/dw_core_sql";
    public static Long projectId = 404L;
    public static List<String> packages = Arrays.asList("com.siheng.dwd.dim", "com.siheng.dwd.fact", "com.siheng.dwd.binlog",
            "com.siheng.dwm", "com.siheng.dws", "com.siheng.dwt", "com.siheng.market.wms", "com.siheng.ads");


    public static Map<String, List<Diff>> getUpdateDiff(String start, String end, String branch) throws Exception {
        Map<String, List<Diff>> result = new HashMap<>();
        GitLabApi gitLabApi = new GitLabApi(gitlabUrl, personalAccessToken);
        gitLabApi.getMergeRequestApi().getMergeRequests(new MergeRequestFilter()
                .withState(Constants.MergeRequestState.MERGED)
                .withProjectId(projectId)
                .withTargetBranch(branch)
                .withCreatedAfter(new SimpleDateFormat("yyyy-MM-dd").parse(start))
                .withCreatedBefore(new SimpleDateFormat("yyyy-MM-dd").parse(end))
        ).forEach(merge -> {
            try {
                result.put(merge.getMergeCommitSha(), gitLabApi.getCommitsApi().getDiff(projectPath, merge.getMergeCommitSha()));
            } catch (GitLabApiException ex) {
                throw new RuntimeException(ex);
            }
        });
        return result;
    }

    public static String getGitFileContent(String filePath, String branch) {

        GitLabApi gitLabApi = new GitLabApi(gitlabUrl, personalAccessToken);

        try {
            RepositoryFile file = gitLabApi.getRepositoryFileApi().getFile(projectId, filePath, branch);
            String escapedContent = file.getContent();

            byte[] decodedBytes = Base64.getDecoder().decode(escapedContent);
            String decodedContent = new String(decodedBytes);
            gitLabApi.close();
            return decodedContent;
        } catch (Exception e) {
            if (!e.getMessage().equals("404 File Not Found")) e.printStackTrace();
        }
        return null;
    }


    public static Set<String> getAllFilePath(String branchName) {
        // 设置GitLab服务器URL和访问令牌
        GitLabApi gitLabApi = new GitLabApi(gitlabUrl, personalAccessToken);
        Set<String> fileNames = new HashSet<>();
        packages
                .forEach(filePath -> {
                    try {
                        gitLabApi.getRepositoryApi().getTree(projectPath, filePath, branchName, true)
                                .forEach(e -> fileNames.add(e.getPath()));
                    } catch (GitLabApiException e) {
                        throw new RuntimeException(e);
                    }
                });
        return fileNames;
    }
}
