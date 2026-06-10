package com.blogcommon.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteResultErrorDecoderTest {
    @Test
    void decodeShouldUseResultMessageWhenBodyMatchesResultShape() {
        RemoteResultErrorDecoder decoder = new RemoteResultErrorDecoder(new ObjectMapper());
        Exception exception = decoder.decode("UserClient#get", response(400, "{\"code\":400,\"message\":\"bad request\"}"));

        RemoteCallException remote = assertInstanceOf(RemoteCallException.class, exception);
        assertEquals(400, remote.getStatus());
        assertEquals("远程服务错误: bad request", remote.getMessage());
    }

    @Test
    void decodeShouldUseRawBodyWhenBodyIsNotJson() {
        RemoteResultErrorDecoder decoder = new RemoteResultErrorDecoder(new ObjectMapper());
        Exception exception = decoder.decode("UserClient#get", response(503, "service unavailable"));

        assertTrue(exception.getMessage().contains("service unavailable"));
    }

    @Test
    void decodeShouldHandleEmptyBody() {
        RemoteResultErrorDecoder decoder = new RemoteResultErrorDecoder(new ObjectMapper());
        Exception exception = decoder.decode("UserClient#get", response(500, ""));

        assertEquals("远程服务调用失败: 500", exception.getMessage());
    }

    private Response response(int status, String body) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/test",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null
        );
        return Response.builder()
                .status(status)
                .request(request)
                .body(body, StandardCharsets.UTF_8)
                .build();
    }
}
