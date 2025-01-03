package cn.lrnev.clientstartverify;

import cn.lrnev.clientstartverify.verify.DefaultStartVerifier;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * @author 鲁子狄
 * @since 2024/12/2 11:26
 **/
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(name = "start-verify.enabled", havingValue = "true", matchIfMissing = true)
public class ClientStartVerifyAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("ClientStartVerifyAutoConfiguration is being initialized.");
    }

    @Bean
    public CommandLineRunner clientStartVerifyRunner(ConfigurableApplicationContext context) {
        return args -> {
            try {
                log.info("Starting client verification process...");
                new DefaultStartVerifier(context).verify();
                log.info("Client verification completed successfully.");
            } catch (Exception e) {
                log.error("Failed to perform start verification: {}", e.getMessage(), e);
                SpringApplication.exit(context, () -> 1);
            }
        };
    }
}