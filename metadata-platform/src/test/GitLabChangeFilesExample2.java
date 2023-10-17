import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.Diff;
import org.gitlab4j.api.utils.ISO8601;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Dearest
 * @date 2023/9/7 3:45 下午
 * @Desc 获取一段时间的变更内容
 */
public class GitLabChangeFilesExample2 {

    public static void main(String[] args) throws Exception {
        // 设置GitLab服务器URL和访问令牌
        String gitlabUrl = "http://git.server.oigbuy.com";
        String personalAccessToken = "aesXQc8YrY-SyXiMWwRc";

        GitLabApi gitLabApi = new GitLabApi(gitlabUrl, personalAccessToken);


        // 指定分支名称和起止时间
        String branchName = "test";
        Date startDate = new SimpleDateFormat("yyyyMMdd").parse("20220101");
        Date endDate = new SimpleDateFormat("yyyyMMdd").parse("20231231");

        // 获取分支的变更提交列表
        List<Commit> commits1 = gitLabApi.getCommitsApi().getCommits("404", branchName, startDate, endDate);
        List<Commit> commits =  commits1.stream().filter(line -> !line.getMessage().contains("Merge"))
                .collect(Collectors.toList());
//        System.out.println(commits);
        // 存储文件名称的集合，用于去重
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

//        List<String> collect = fileNames.stream().filter(line -> line.startsWith("com.siheng.dws"))
//                .collect(Collectors.toList());

        List<String> packages = Arrays.asList("com.siheng.ads",
                "com.siheng.dwd.binlog",
                "com.siheng.dwd.dim",
                "com.siheng.dwd.fact",
                "com.siheng.dwm",
                "com.siheng.dws",
                "com.siheng.dwt",
                "com.siheng.market.replenishment");


        fileNames.removeIf(element -> packages.stream().noneMatch(element::startsWith));

        for (String fileName : fileNames) {
            System.out.println(fileName);
        }

    }
}
