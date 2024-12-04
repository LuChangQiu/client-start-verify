package cn.lrnev.clientstartverify.conf;

import cn.lrnev.clientstartverify.verify.DefaultStartVerifier;
import cn.lrnev.clientstartverify.verify.StartVerifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置
 *
 * @author 鲁子狄
 * @since 2024/12/4 13:36
 **/
@Configuration
public class StartVerifierConfiguration {

    private final ConfigurableApplicationContext context;

    public StartVerifierConfiguration(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @Bean
    @ConditionalOnMissingBean(StartVerifier.class)
    public StartVerifier startVerifier() {
        return new DefaultStartVerifier(context);
    }
}