package cn.lrnev.clientstartverify.verify;

import cn.lrnev.clientstartverify.core.R;
import cn.lrnev.clientstartverify.detector.OperatingSystemDetector;
import com.dtflys.forest.Forest;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

/**
 * 客户端启动默认校验
 *
 * @author 鲁子狄
 * @since 2024/12/2 11:28
 **/
@Slf4j
@Component
public class DefaultStartVerifier implements StartVerifier {

    private final ConfigurableApplicationContext context;

    public DefaultStartVerifier(ConfigurableApplicationContext context) {
        this.context = context;
    }

    /**
     * 启动时验证客户端信息的方法。
     */
    @Override
    public void verify() throws IllegalArgumentException {
        ConfigurableEnvironment env = context.getEnvironment();

        StarterRequest request = OperatingSystemDetector.getOperatingSystemInfo(env);
        String secretKey = env.getProperty("start-verify.secretKey");
        if (StringUtils.isNotEmpty(secretKey)) {
            request.setSecretKey(secretKey);
        }
        try {
            R<?> response = Forest.post(env.getProperty("start-verify.url"))
                    .contentType("application/json")
                    .addBody(request)
                    .execute(R.class);
            if (Boolean.FALSE.equals(R.isSuccess(response))) {
                log.warn(response.getMsg());
                exitWithError();
            } else {
                log.info("Client start verification successful.");
            }
        } catch (Exception e) {
            log.error("Unable to connect to the authentication service: {}", e.getMessage());
            exitWithError();
        }
    }

    /**
     * 以非零退出码退出应用程序，表示启动失败。
     */
    private void exitWithError() {
        SpringApplication.exit(context, () -> 1);
        System.exit(1);
    }
}
