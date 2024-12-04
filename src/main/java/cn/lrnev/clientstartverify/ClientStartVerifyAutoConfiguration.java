package cn.lrnev.clientstartverify;

import cn.lrnev.clientstartverify.verify.StartVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@Slf4j
@Configuration
@ConditionalOnProperty(name = "start-verify.enabled", havingValue = "true", matchIfMissing = true)
public class ClientStartVerifyAutoConfiguration {

    private final StartVerifier startVerifier;

    public ClientStartVerifyAutoConfiguration(StartVerifier startVerifier) {
        this.startVerifier = startVerifier;
    }

    @Bean
    public CommandLineRunner clientStartVerifyRunner(ConfigurableApplicationContext context) {
        return args -> {
            try {
                log.info("Starting client verification process...");
                startVerifier.verify();
                log.info("Client verification completed successfully.");
            } catch (Exception e) {
                log.error("Failed to perform start verification: {}", e.getMessage(), e);
                SpringApplication.exit(context, () -> 1);
            }
        };
    }
}