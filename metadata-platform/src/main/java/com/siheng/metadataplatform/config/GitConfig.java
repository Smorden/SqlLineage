package com.siheng.metadataplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Dearest
 * @date 2023/9/5 5:08 下午
 * @Desc
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "siheng.gitlab")
public class GitConfig {

    private String host;
    private String accessToken;
    private String namespace;
    private String projectName;
    private String branch;
}