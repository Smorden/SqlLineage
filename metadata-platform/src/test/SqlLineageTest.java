import com.siheng.metadataplatform.utils.SqlLineageUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * @author Dearest
 * @date 2023/9/12 9:54 上午
 * @Desc
 */
public class SqlLineageTest {
    public static void main(String[] args) throws Exception {
        File folder = new File("D:\\idea\\workspace\\dw_core_sql\\com.siheng.dws");
        File[] listOfFiles = folder.listFiles();

        List<String> myList = new ArrayList<String>(Arrays.asList("dws_itop_amazon_asin_warning_inform_di.sql"));

        for (File file : listOfFiles) {
            if (myList.contains(file.getName())) {
                System.out.println("正在执行的文件是" + file.getName());
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();
                StringBuffer sb = new StringBuffer();
                while (line != null) {
                    sb.append(line);
                    sb.append(System.lineSeparator());
                    line = reader.readLine();
                }
                String fileContents = sb.toString();
                String result = StringUtils.substringAfterLast(fileContents, "-- begin_insert --")
                        .replace("[ broadcast ]", " ")
                        .replaceAll("with.*label.*@label", "")
                        .replaceAll(";", "")
                        .replaceAll("WITH.*label.*@label", "");

                    Map<String, Set<String>> stringSetMap = SqlLineageUtil.sqlParser(result);
                    System.out.println(stringSetMap);

            }
        }
    }
}
