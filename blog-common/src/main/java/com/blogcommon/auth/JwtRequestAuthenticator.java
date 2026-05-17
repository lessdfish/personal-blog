package com.blogcommon.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtRequestAuthenticator {
    private final JwtAuthSupport jwtAuthSupport;
    private final TokenSessionValidator tokenSessionValidator;

    public JwtRequestAuthenticator(JwtAuthSupport jwtAuthSupport, TokenSessionValidator tokenSessionValidator) {
        this.jwtAuthSupport = jwtAuthSupport;
        this.tokenSessionValidator = tokenSessionValidator;
    }

    public JwtUserInfo authenticate(HttpServletRequest request, HttpServletResponse response,
                                    int unauthorizedCode, String unauthorizedMessage,
                                    int invalidCode, String invalidMessage,
                                    String inactiveMessage) throws IOException {
        JwtUserInfo userInfo = jwtAuthSupport.parseRequiredUser(
                request,
                response,
                unauthorizedCode,
                unauthorizedMessage,
                invalidCode,
                invalidMessage
        );
        if (userInfo == null) {
            return null;
        }
        if (!tokenSessionValidator.isTokenActive(userInfo.userId(), userInfo.token())) {
            writeJson(response, invalidCode, inactiveMessage);
            return null;
        }
        return userInfo;
    }

    public void clear() {
        jwtAuthSupport.clear();
    }

    private void writeJson(HttpServletResponse response, int code, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}
