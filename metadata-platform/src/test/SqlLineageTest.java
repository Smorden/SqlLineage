import com.siheng.metadataplatform.utils.SqlLineageUtil;

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
        File folder = new File("/Users/shiyuchao/work/workspace/dw_core_sql/com.siheng.dwd.dim");
        File[] listOfFiles = folder.listFiles();

        List<String> myList = new ArrayList<String>(Arrays.asList("dwd_dim_sales_channel_sku_site_dep_ds.sql"));

        for (File file : listOfFiles) {
            if (myList.contains(file.getName())) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();
                StringBuffer sb = new StringBuffer();
                while(line != null) {
                    sb.append(line);
                    sb.append(System.lineSeparator());
                    line = reader.readLine();
                }


                String fileContents = sb.toString();

                String keyword = "-- begin_insert --";
                int index = fileContents.indexOf(keyword);

                if (index != -1 && index + keyword.length() < fileContents.length()) {
                    String result = fileContents.substring(index + keyword.length());
                    Map<String, Set<String>> stringSetMap = SqlLineageUtil.sqlParser(result);
                    System.out.println(stringSetMap);
                }

            }
        }
    }
}
