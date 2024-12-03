package cn.lrnev.clientstartverify.verify;

import lombok.Data;

/**
 *
 * 启动参数
 *
 * @author 鲁子狄
 * @since 2024/12/2 16:01
 **/
@Data
public class StarterRequest {
    /**
     * 主板序列号
     */
    private String mbSerialNo;

    /**
     * cpu序列号
     */
    private String cpuSerialNo;

    /**
     * 内存序列号
     */
    private String memorySerialNo;

    /**
     * 硬盘序列号
     */
    private String diskSerialNo;

    /**
     * 网卡序列号/MAC地址
     */
    private String nicSerialNo;

    /**
     * ip
     */
    private String ip;

    /**
     * 端口
     */
    private String port;

    /**
     * 客户
     */
    private String customer;

    /**
     * 项目
     */
    private String project;

    /**
     * 密钥
     */
    private String secretKey;
}