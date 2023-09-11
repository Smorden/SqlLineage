import org.apache.commons.lang3.StringEscapeUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.RepositoryFile;

import java.util.Base64;

public class GitLabChangeFilesExample {

    public static void main(String[] args) {

        // 设置GitLab服务器URL和访问令牌
        String gitlabUrl = "http://git.server.oigbuy.com";
        String personalAccessToken = "aesXQc8YrY-SyXiMWwRc";

        // 实例化GitLabApi对象，并设置访问令牌
        GitLabApi gitLabApi = new GitLabApi(gitlabUrl, personalAccessToken);

        try {
            // 获取某个仓库下某个文件的内容
            RepositoryFile file = gitLabApi.getRepositoryFileApi().getFile("404", "com.siheng.dws/dws_ivct_goods_warehouse_stock_ds.sql", "test");
            String escapedContent = file.getContent();

            byte[] decodedBytes = Base64.getDecoder().decode(escapedContent);
            String decodedContent = new String(decodedBytes);

            // 输出文件内容
            System.out.println(decodedContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
