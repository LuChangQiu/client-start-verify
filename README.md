`client-start-verify` 用于在启动时验证客户端信息的模块，以确保只有经过授权的服务器才能启动服务。期间会根据 **主板序列号/cpu序列号/内存序列号/硬盘序列号/网卡序列号&MAC地址/ip/端口/客户名称/项目/密钥** 去进行验证.



## 第一步做什么？



1. `java` 版本 `17`

2. 添加依赖

   - \`Maven` 依赖

   ```xml
   <dependency>
     <groupId>cn.lrnev</groupId>
     <artifactId>client-start-verify</artifactId>
     <version>1.0.0</version>
   </dependency>
   ```

   - `Gradle` 依赖

   ```xml
   dependencies {
     compile 'cn.lrnev:client-start-verify:1.0.0'
   }
   ```

3. 添加配置文件内容 `application.yml`

   添加上依赖后默认 `enabled` 是启动的

   ```yaml
   start-verify:
     enabled: false
     customer: Mada
     project: GGSC
     url: http://localhost:9011/client/startVerify
   ```

4. 配置文件包含参数

   ```java
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
   ```

5. 应用程序启动时自动获取 `secretKey`

   - `secretKey` 可以配置在配置文件中(不推荐)
   - 环境变量传参  `java-command: java -Dstart-verify.secretKey=1!v*wQVsUyLSPDv6 -jar admin.jar` (推荐)

6. 重写 `DefaultStartVerifier`

   ```java
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
   ```

   

