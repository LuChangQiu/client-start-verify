package cn.lrnev.clientstartverify.verify;

import cn.hutool.extra.spring.SpringUtil;
import cn.lrnev.clientstartverify.core.R;
import cn.lrnev.clientstartverify.detector.OperatingSystemDetector;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;


/**
 * 客户端启动校验
 *
 * @author 鲁子狄
 * @since 2024/12/2 11:28
 **/
@Slf4j
@SuppressWarnings("unchecked")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientStartVerify {


    /**
     * 启动时验证客户端信息的方法。
     *
     * @param context Spring应用上下文，用于获取环境配置和执行退出操作。
     */
    public static void start(ConfigurableApplicationContext context) {

        ConfigurableEnvironment env = context.getEnvironment();

        StarterRequest request = OperatingSystemDetector.getOperatingSystemInfo(env);
        String secretKey = env.getProperty("secretKey");
        if (StringUtils.isNotEmpty(secretKey)) {
            request.setSecretKey(secretKey);
        }
        try {
            R<Void> response = SpringUtil.getBean(StarterClient.class).startVerify(request);
            if (Boolean.FALSE.equals(R.isSuccess(response))) {
                log.warn(response.getMsg());
                exitWithError(context);
            }
        } catch (Exception e) {
            log.error("Unable to connect to the authentication service: {}", e.getMessage());
            exitWithError(context);
        }
    }

    /**
     * 以非零退出码退出应用程序，表示启动失败。
     *
     * @param context Spring应用上下文，用于执行退出操作
     */
    private static void exitWithError(ConfigurableApplicationContext context) {
        SpringApplication.exit(context, () -> 1);
    }
}