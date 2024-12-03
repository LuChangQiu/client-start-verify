package cn.lrnev.clientstartverify;

import cn.lrnev.clientstartverify.verify.ClientStartVerify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 鲁子狄
 * @since 2024/12/2 11:26
 **/
@Configuration
@ConditionalOnProperty(name = "start-verify.enabled", havingValue = "true", matchIfMissing = true)
public class ClientStartVerifyAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ClientStartVerifyAutoConfiguration.class);

    @Bean
    public CommandLineRunner clientStartVerifyRunner(ConfigurableApplicationContext context) {
        return args -> {
            try {
                ClientStartVerify.start(context);
            } catch (Exception e) {
                log.error("Failed to perform start verification: {}", e.getMessage(), e);
                SpringApplication.exit(context, () -> 1);
            }
        };
    }
}