package com.siheng.metadataplatform.utils;

import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.Diff;
import org.gitlab4j.api.models.RepositoryFile;
import org.jvnet.hk2.annotations.Optional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Dearest
 * @date 2023/9/5 4:14 下午
 * @Desc
 */
public class GitUtil {

    public static String getGitFileContent(String filePath, String branch) {

        String gitlabUrl = "http://git.server.oigbuy.com";
        String personalAccessToken = "aesXQc8YrY-SyXiMWwRc";

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


    public static Set<String> getAllModifyFilePath(String start, String end, String branchName) throws Exception {
        // 设置GitLab服务器URL和访问令牌
        String gitlabUrl = "http://git.server.oigbuy.com";
        String personalAccessToken = "aesXQc8YrY-SyXiMWwRc";

        GitLabApi gitLabApi = new GitLabApi(gitlabUrl, personalAccessToken);


        // 指定分支名称和起止时间
        Date startDate = new SimpleDateFormat("yyyy-MM-dd").parse(StringUtils.defaultIfBlank(start,"2022-01-01"));
        Date endDate = new SimpleDateFormat("yyyy-MM-dd").parse(StringUtils.defaultIfBlank(end,"2099-12-31"));

        // 获取分支的变更提交列表
        List<Commit> allCommits = gitLabApi.getCommitsApi().getCommits("404", branchName, startDate, endDate);
        List<Commit> commits = allCommits.stream().filter(line -> !line.getMessage().contains("Merge"))
                .collect(Collectors.toList());

        Set<String> fileNames = new HashSet<>();
        for (Commit commit : commits) {
            // 获取每个提交的变更文件列表
            List<Diff> diffs = gitLabApi.getCommitsApi().getDiff("404", commit.getId());

            // 提取文件名称（包括包名）
            for (Diff diff : diffs) {
                String fileName = diff.getNewPath();
                // 添加到文件名称集合
                fileNames.add(fileName);
            }
        }
        List<String> packages = Arrays.asList("com.siheng.dwd.dim", "com.siheng.dwd.fact", "com.siheng.dwd.binlog", "com.siheng.dwm", "com.siheng.dws", "com.siheng.dwt", "com.siheng.market.wms");

        fileNames.removeIf(element -> packages.stream().noneMatch(element::startsWith));
        gitLabApi.close();
        return fileNames;

    }


}
