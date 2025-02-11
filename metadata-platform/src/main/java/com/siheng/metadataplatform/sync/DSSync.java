package com.siheng.metadataplatform.sync;

import com.siheng.metadataplatform.service.DSService;
import com.siheng.metadataplatform.utils.SqlLineageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static com.siheng.metadataplatform.constant.CustomConstant.sharedData;

/**
 * @Author: ma.shuai
 * @CreateTime: 2024-12-10  14:56
 * @Description: 独立线程处理异步处理gitlab的merge请求
 */
@Component
public class DSSync {
    @Autowired
    private DSService dSService;
    private Thread myThread;

    @PostConstruct
    public void startThread() {
        myThread = new Thread(() -> {
            while (true) {
                try {
                    if (sharedData) {
                        if (SqlLineageUtil.queue.size() > 0) {
                            String uniqueKey = SqlLineageUtil.queue.peek();
                            dSService.updateProcess(uniqueKey);
                        }
                    } else {
                        System.out.println("暂停执行!!!");
                        Thread.sleep(5000L);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        myThread.start();
    }

}
