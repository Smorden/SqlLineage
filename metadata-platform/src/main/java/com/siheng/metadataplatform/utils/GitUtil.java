package com.siheng.metadataplatform.utils;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.RepositoryFile;

import java.util.*;

/**
 * @author Dearest
 * @date 2023/9/5 4:14 下午
 * @Desc
 */
public class GitUtil {

    public static String gitlabUrl = "http://git.server.oigbuy.com";
    public static String personalAccessToken = "-qnhcgHjSnzQ-uqASU7s";
    public static String projectIdOrPath = "bigdata/dw_core_sql";
    public static List<String> packages = Arrays.asList("com.siheng.dwd.dim", "com.siheng.dwd.fact", "com.siheng.dwd.binlog",
            "com.siheng.dwm", "com.siheng.dws", "com.siheng.dwt", "com.siheng.market.wms", "com.siheng.ads");
    public static String getGitFileContent(String filePath, String branch) {

        GitLabApi gitLabApi = new GitLabApi(gitlabUrl, personalAccessToken);

        try {
            RepositoryFile file = gitLabApi.getRepositoryFileApi().getFile("404", filePath, branch);
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


    public static Set<String> getAllModifyFilePath(String branchName) throws Exception {
        // 设置GitLab服务器URL和访问令牌
        GitLabApi gitLabApi = new GitLabApi(gitlabUrl, personalAccessToken);
        Set<String> fileNames = new HashSet<>();
        packages
                .forEach(filePath -> {
                    try {
                        gitLabApi.getRepositoryApi().getTree(projectIdOrPath, filePath, branchName, true)
                                .forEach(e -> fileNames.add(e.getPath()));
                    } catch (GitLabApiException e) {
                        throw new RuntimeException(e);
                    }
                });
        return fileNames;
    }
}
