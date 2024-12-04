package cn.lrnev.clientstartverify.verify;

/**
 * 启动校验接口
 *
 * @author 鲁子狄
 * @since 2024/12/4 10:53
 **/
public interface StartVerifier {

    /**
     * 启动时验证客户端信息的方法。
     */
    void verify() throws IllegalArgumentException;
}