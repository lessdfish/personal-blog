package com.blogcommon.feign;

/**
 * 远程调用异常：保留目标服务、方法、HTTP 状态码和响应摘要，供降级和日志使用。
 */
public class RemoteCallException extends RuntimeException {
    private final String methodKey;
    private final int status;
    private final String responseBody;

    public RemoteCallException(String methodKey, int status, String responseBody, String message) {
        super(message);
        this.methodKey = methodKey;
        this.status = status;
        this.responseBody = responseBody;
    }

    public String getMethodKey() {
        return methodKey;
    }

    public int getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
