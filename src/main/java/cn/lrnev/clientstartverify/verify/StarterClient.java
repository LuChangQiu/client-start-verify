package cn.lrnev.clientstartverify.verify;

import cn.lrnev.clientstartverify.core.R;
import com.dtflys.forest.annotation.BaseRequest;
import com.dtflys.forest.annotation.Body;
import com.dtflys.forest.annotation.PostRequest;
import org.springframework.stereotype.Component;

/**
 * 启动接口
 *
 * @author 鲁子狄
 * @since 2024/12/2 17:03
 **/
@Component
@BaseRequest(baseURL = "${client.start.verify.api_url}")
public class StarterClient {

    @PostRequest(url = "/client/startVerify", contentType = "application/json")
    public R<Void> startVerify(@Body StarterRequest request) {
        // 这里可以添加额外的逻辑，比如日志记录、异常处理等
        return null;
    }
}