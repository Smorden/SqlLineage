/**
 * @author Shinya
 * @date 2023/7/10 7:38 下午
 * @Desc
 */

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class TestFileGet {

    public static List<String> readRemoteFolder(String user, String password, String host, int port, String remoteFolder, boolean modifiedTodayOnly) {
        List<String> fileContents = new ArrayList<>();
        LocalDate today = LocalDate.now();

        int i = 0;
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            Vector<ChannelSftp.LsEntry> fileList = channelSftp.ls(remoteFolder);
            for (ChannelSftp.LsEntry file : fileList) {
                if (!file.getAttrs().isDir() && file.getFilename().endsWith(".sql") && !file.getFilename().contains("test") && !file.getFilename().contains("xxx")) {
                    if (modifiedTodayOnly) {
                        Date modifiedDate = new Date(file.getAttrs().getMTime() * 1000L);
                        LocalDate modifiedLocalDate = modifiedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        if (!modifiedLocalDate.equals(today)) {
                            continue;
                        }
                    }
                    StringBuffer stringBuffer = new StringBuffer();
                    InputStream inputStream = channelSftp.get(remoteFolder + "/" + file.getFilename());
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    System.out.println(file.getFilename() + "   " + ++i);
//                    System.out.println(file.getFilename());
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuffer.append(line).append("\n");
                    }
                    fileContents.add(stringBuffer.toString());
                    bufferedReader.close();
                    inputStream.close();
                }
            }

            channelSftp.disconnect();
            session.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return fileContents;
    }


    public static void main(String[] args) {
        List<String> allFileContents = TestFileGet.readRemoteFolder("admin", "admin", "ds1.service-test.oigbuy.com", 22, "/opt/module/datax/script", false);
        System.out.println(allFileContents);
    }
}
