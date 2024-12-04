package cn.lrnev.clientstartverify;

import cn.lrnev.clientstartverify.verify.DefaultStartVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;


@ExtendWith(SpringExtension.class)
class DefaultStartVerifierAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ClientStartVerifyAutoConfiguration.class));

    /**
     * 测试当属性 start-verify.enabled 设置为 true 时，clientStartVerifyRunner Bean 是否被创建。
     */
    @Test
    void testClientStartVerifyRunnerIsCreatedWhenPropertyIsTrue() {
        contextRunner
                .withPropertyValues("start-verify.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(CommandLineRunner.class));
    }

    /**
     * 测试当属性 start-verify.enabled 缺失时，clientStartVerifyRunner Bean 是否仍然被创建。
     */
    @Test
    void testClientStartVerifyRunnerIsCreatedWhenPropertyIsMissing() {
        contextRunner
                .run(context -> assertThat(context).hasSingleBean(CommandLineRunner.class));
    }

    /**
     * 测试当属性 start-verify.enabled 设置为 false 时，clientStartVerifyRunner Bean 不会被创建。
     */
    @Test
    void testClientStartVerifyRunnerIsNotCreatedWhenPropertyIsFalse() {
        contextRunner
                .withPropertyValues("start-verify.enabled=false")
                .run(context -> assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
                        .isThrownBy(() -> context.getBean(CommandLineRunner.class)));
    }

    @Test
    void testClientStartVerifyRunnerBehavior() {
        // 模拟 ConfigurableApplicationContext 对象
        ConfigurableApplicationContext mockContext = mock(ConfigurableApplicationContext.class);

        // 模拟 Environment 对象
        ConfigurableEnvironment mockEnvironment = mock(ConfigurableEnvironment.class);
        when(mockContext.getEnvironment()).thenReturn(mockEnvironment);
        when(mockEnvironment.getProperty("start-verify.customer")).thenReturn("测试公司");
        when(mockEnvironment.getProperty("start-verify.project")).thenReturn("测试项目");
        when(mockEnvironment.getProperty("start-verify.enabled")).thenReturn("true");
        when(mockEnvironment.getProperty("start-verify.url")).thenReturn("http://localhost:9011/");

        // 设置环境属性（如果必要）
        when(mockEnvironment.getProperty("some.property")).thenReturn("propertyValue");

        contextRunner
                .withBean(ConfigurableApplicationContext.class, () -> mockContext)
                .withUserConfiguration(MockClientStartVerifyConfiguration.class)
                // 提供必要的配置属性
                .withPropertyValues("some.property=propertyValue")
                .run(context -> {
                    CommandLineRunner runner = context.getBean(CommandLineRunner.class);

                    // 从应用上下文中获取模拟的 DefaultStartVerifier Bean
                    DefaultStartVerifier mockDefaultStartVerifier = context.getBean(DefaultStartVerifier.class);

                    // 执行 CommandLineRunner 的 run 方法
                    runner.run();

                    // 验证 DefaultStartVerifier.start 方法是否被调用一次，并传入了正确的参数
                    verify(mockDefaultStartVerifier, times(1)).verify();
                });
    }

    /**
     * 自定义配置类，用于向测试中注入模拟的 DefaultStartVerifier Bean。
     */
    @Configuration
    static class MockClientStartVerifyConfiguration {
        @Bean
        public DefaultStartVerifier mockClientStartVerify() {
            return mock(DefaultStartVerifier.class);
        }
    }
}