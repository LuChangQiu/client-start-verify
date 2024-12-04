package cn.lrnev.clientstartverify.core;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 配置类
 *
 * @author 鲁子狄
 * @since 2024/12/4 10:37
 **/
@Data
@Component
@ConfigurationProperties(prefix = "start-verify")
public class StartVerifyProperties {
    /**
     * 客户名称
     */
    private String customer;

    /**
     * 项目名称
     */
    private String project;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 接口地址
     */
    private String url;

    /**
     * 密钥
     */
    private String secretKey;
}