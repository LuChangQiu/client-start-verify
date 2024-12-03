package cn.lrnev.clientstartverify.detector;

import cn.lrnev.clientstartverify.verify.StarterRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 操作系统检测器
 *
 * @author 鲁子狄
 * @since 2024/12/2 16:01
 **/
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OperatingSystemDetector {

    private static final String WMIC_COMMAND_PREFIX = "wmic ";
    private static final String SERIAL_NUMBER = "SerialNumber";
    private static final String WIN = "win";

    // 是否使用sudo，可以通过配置文件或环境变量设置
    private static final boolean USE_SUDO = true;
    // 命令执行超时时间
    private static final int COMMAND_TIMEOUT_SECONDS = 10;

    /**
     * 获取操作系统信息并填充StarterRequest对象。
     *
     * @return 包含操作系统信息的StarterRequest对象
     */
    public static StarterRequest getOperatingSystemInfo(ConfigurableEnvironment env) {
        StarterRequest request = new StarterRequest();
        String osName = System.getProperty("os.name").toLowerCase();

        CompletableFuture<Void> mbSerialNoFuture = CompletableFuture.supplyAsync(() ->
                        osName.contains(WIN) ? getMotherboardSerialNumberWindows() : getMotherboardSerialNumberLinux())
                .thenApply(optional -> optional.orElse(""))
                .thenAccept(request::setMbSerialNo);

        CompletableFuture<Void> cpuSerialNoFuture = CompletableFuture.supplyAsync(() ->
                        osName.contains(WIN) ? getCpuSerialNumberWindows() : getCpuSerialNumberLinux())
                .thenApply(optional -> optional.orElse(""))
                .thenAccept(request::setCpuSerialNo);

        CompletableFuture<Void> memorySerialNoFuture = CompletableFuture.supplyAsync(() ->
                        osName.contains(WIN) ? getMemorySerialNumbersWindows() : getMemorySerialNumbersLinux())
                .thenApply(optional -> optional.orElse(""))
                .thenAccept(request::setMemorySerialNo);

        CompletableFuture<Void> diskSerialNoFuture = CompletableFuture.supplyAsync(() ->
                        osName.contains(WIN) ? getDiskSerialNumbersWindows() : getDiskSerialNumbersLinux())
                .thenApply(optional -> optional.orElse(""))
                .thenAccept(request::setDiskSerialNo);

        CompletableFuture<Void> nicSerialNoFuture = CompletableFuture.supplyAsync(() ->
                        osName.contains(WIN) ? getNicSerialNumbersWindows() : getNicSerialNumbersLinux())
                .thenApply(optional -> optional.orElse(""))
                .thenAccept(request::setNicSerialNo);

        // 等待所有异步任务完成
        CompletableFuture.allOf(mbSerialNoFuture, cpuSerialNoFuture, memorySerialNoFuture, diskSerialNoFuture, nicSerialNoFuture).join();

        // 设置IP地址
        request.setIp(getLocalIp().orElse(""));
        request.setPort(env.getProperty("server.port"));
        request.setCustomer(env.getProperty("start-verify.customer"));
        request.setProject(env.getProperty("start-verify.project"));

        return request;
    }

    /**
     * 执行命令并返回结果流，带超时机制。
     *
     * @param command 命令
     * @return 命令输出的流
     */
    private static Stream<String> executeCommandWithTimeout(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            if (!process.waitFor(OperatingSystemDetector.COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroy();
                log.warn("Command '{}' timed out after {} seconds", command, OperatingSystemDetector.COMMAND_TIMEOUT_SECONDS);
                return Stream.empty();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            // 启动一个线程来读取错误流，避免阻塞
            Thread errorThread = new Thread(() -> errorReader.lines().forEach(line -> log.error("Error: {}", line)));
            errorThread.start();

            return reader.lines();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Command execution was interrupted: {}", command);
            return Stream.empty();
        } catch (Exception e) {
            log.error("Error executing command: {}", command, e);
            return Stream.empty();
        }
    }

    /**
     * 执行命令并过滤掉空行和去空格。
     *
     * @param command 命令
     * @return 过滤后的命令输出流
     */
    private static Stream<String> executeCommandAndFilter(String command) {
        return executeCommandWithTimeout(command)
                .filter(line -> !line.trim().isEmpty())
                .map(String::trim);
    }

    /**
     * 获取主板序列号（Windows系统）。
     *
     * @return 主板序列号的Optional对象，如果获取失败则返回空Optional
     */
    private static Optional<String> getMotherboardSerialNumberWindows() {
        return executeCommandAndFilter(WMIC_COMMAND_PREFIX + "baseboard get serialnumber")
                .filter(line -> !SERIAL_NUMBER.equalsIgnoreCase(line))
                .findFirst();
    }

    /**
     * 获取CPU序列号（Windows系统）。
     *
     * @return CPU序列号的Optional对象，如果获取失败则返回空Optional
     */
    private static Optional<String> getCpuSerialNumberWindows() {
        // 执行命令并过滤掉标题行，收集所有非空的处理器ID
        String cpuSerialNumbers = executeCommandAndFilter(WMIC_COMMAND_PREFIX + "cpu get ProcessorId")
                .filter(line -> !"ProcessorId".equalsIgnoreCase(line.trim()))
                .map(String::trim)
                .collect(Collectors.joining(", "));
        // 如果结果为空，则返回空Optional，否则返回包含拼接结果的Optional
        return cpuSerialNumbers.isEmpty() ? Optional.empty() : Optional.of(cpuSerialNumbers);
    }

    /**
     * 获取所有内存模块的序列号（Windows系统）。
     * 如果有多个内存模块，返回以逗号分隔的字符串。
     *
     * @return 内存序列号的Optional对象，如果获取失败则返回空Optional
     */
    private static Optional<String> getMemorySerialNumbersWindows() {
        return executeCommandAndFilter(WMIC_COMMAND_PREFIX + "memorychip get SerialNumber")
                .filter(line -> !SERIAL_NUMBER.equalsIgnoreCase(line))
                .collect(Collectors.joining(", "))
                .isEmpty() ? Optional.empty() : Optional.of(executeCommandAndFilter(WMIC_COMMAND_PREFIX + "memorychip get SerialNumber")
                .filter(line -> !SERIAL_NUMBER.equalsIgnoreCase(line))
                .collect(Collectors.joining(", ")));
    }

    /**
     * 获取所有硬盘的序列号（Windows系统）。
     * 如果有多个硬盘，返回以逗号分隔的字符串。
     *
     * @return 硬盘序列号的Optional对象，如果获取失败则返回空Optional
     */
    private static Optional<String> getDiskSerialNumbersWindows() {
        return executeCommandAndFilter(WMIC_COMMAND_PREFIX + "diskdrive get SerialNumber")
                .filter(line -> !SERIAL_NUMBER.equalsIgnoreCase(line))
                .collect(Collectors.joining(", "))
                .isEmpty() ? Optional.empty() : Optional.of(executeCommandAndFilter(WMIC_COMMAND_PREFIX + "diskdrive get SerialNumber")
                .filter(line -> !SERIAL_NUMBER.equalsIgnoreCase(line))
                .collect(Collectors.joining(", ")));
    }

    /**
     * 获取所有启用的网卡的MAC地址（Windows系统）。
     * 如果有多个网卡，返回以逗号分隔的字符串。
     *
     * @return 网卡MAC地址的Optional对象，如果获取失败则返回空Optional
     */
    private static Optional<String> getNicSerialNumbersWindows() {
        return executeCommandAndFilter(WMIC_COMMAND_PREFIX + "nic where 'NetEnabled=true' get MACAddress")
                .filter(line -> !"MACAddress".equalsIgnoreCase(line))
                .collect(Collectors.joining(", "))
                .isEmpty() ? Optional.empty() : Optional.of(executeCommandAndFilter(WMIC_COMMAND_PREFIX + "nic where 'NetEnabled=true' get MACAddress")
                .filter(line -> !"MACAddress".equalsIgnoreCase(line))
                .collect(Collectors.joining(", ")));
    }

    /**
     * 获取本地IP地址。
     * 尝试返回第一个非环回接口的IPv4地址。
     *
     * @return 本地IP地址的Optional对象，如果获取失败则返回空Optional
     */
    private static Optional<String> getLocalIp() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            if (!localhost.isLoopbackAddress() && localhost instanceof Inet4Address) {
                return Optional.of(localhost.getHostAddress());
            } else {
                return findNonLoopbackIpv4Address();
            }
        } catch (Exception e) {
            log.warn("Error getting local IP address: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 查找并返回第一个非环回的IPv4地址。
     *
     * @return 第一个找到的非环回IPv4地址的Optional对象，如果没有找到则返回空Optional
     */
    private static Optional<String> findNonLoopbackIpv4Address() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                        return Optional.of(inetAddress.getHostAddress());
                    }
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error iterating network interfaces: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 获取主板序列号（Linux系统）。
     *
     * @return 主板序列号的Optional对象，如果获取失败则返回空Optional
     */
    private static Optional<String> getMotherboardSerialNumberLinux() {
        final String command = USE_SUDO ? "sudo dmidecode -s baseboard-serial-number" : "dmidecode -s baseboard-serial-number";
        return executeCommandAndFilter(command)
                .findFirst();
    }

    /**
     * 获取CPU序列号（Linux系统）。
     *
     * @return CPU序列号的Optional对象，如果获取失败则返回空Optional
     */
    private static Optional<String> getCpuSerialNumberLinux() {
        // 构建命令
        final String command = USE_SUDO ? "sudo dmidecode -t processor | grep ID" : "dmidecode -t processor | grep ID";

        // 执行命令并过滤掉无关行，收集所有非空的处理器ID
        String cpuSerialNumbers = executeCommandAndFilter(command)
                .filter(line -> line.contains("ID"))
                .map(line -> line.split(":")[1].trim())
                .collect(Collectors.joining(", "));

        return Optional.of(cpuSerialNumbers).filter(s -> !s.isEmpty());
    }

    /**
     * 获取所有内存模块的序列号（Linux系统）。
     * 如果有多个内存模块，返回以逗号分隔的字符串。
     *
     * @return 内存序列号的Optional对象，如果获取失败则返回空Optional
     */
    private static Optional<String> getMemorySerialNumbersLinux() {
        final String command = USE_SUDO ? "sudo dmidecode -t memory | grep 'Serial Number'" : "dmidecode -t memory | grep 'Serial Number'";
        return executeCommandAndFilter(command)
                .map(line -> line.split(":")[1].trim())
                .filter(line -> !"not specified".equalsIgnoreCase(line) && !line.isEmpty())
                .collect(Collectors.joining(", "))
                .isEmpty() ? Optional.empty() : Optional.of(executeCommandAndFilter(command)
                .map(line -> line.split(":")[1].trim())
                .filter(line -> !"not specified".equalsIgnoreCase(line) && !line.isEmpty())
                .collect(Collectors.joining(", ")));
    }

    /**
     * 获取所有硬盘的序列号（Linux系统）。
     * 如果有多个硬盘，返回以逗号分隔的字符串。
     *
     * @return 硬盘序列号的Optional对象，如果获取失败则返回空Optional
     */
    private static Optional<String> getDiskSerialNumbersLinux() {
        final String command = USE_SUDO ? "sudo lsblk -ndo SERIAL" : "lsblk -ndo SERIAL";
        return executeCommandAndFilter(command)
                .collect(Collectors.joining(", "))
                .isEmpty() ? Optional.empty() : Optional.of(executeCommandAndFilter(command)
                .collect(Collectors.joining(", ")));
    }

    /**
     * 获取所有启用的网卡的MAC地址（Linux系统）。
     * 如果有多个网卡，返回以逗号分隔的字符串。
     *
     * @return 网卡MAC地址的Optional对象，如果获取失败则返回空Optional
     */
    private static Optional<String> getNicSerialNumbersLinux() {
        return executeCommandAndFilter("ip link show | grep 'link/ether'")
                .map(line -> line.split(" ")[1].trim())
                .collect(Collectors.joining(", "))
                .isEmpty() ? Optional.empty() : Optional.of(executeCommandAndFilter("ip link show | grep 'link/ether'")
                .map(line -> line.split(" ")[1].trim())
                .collect(Collectors.joining(", ")));
    }
}
