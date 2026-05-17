package com.bloggateway.service;

import com.bloggateway.config.IpSecurityProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@Component
public class ClientIpResolver {
    private final IpSecurityProperties properties;

    public ClientIpResolver(IpSecurityProperties properties) {
        this.properties = properties;
    }

    /**
     * 解析客户端 IP：根据远程地址和可信代理头判断真实来源 IP。
     */
    public String resolve(ServerWebExchange exchange) {
        String remoteIp = resolveRemoteAddress(exchange);
        if (isTrustedProxy(remoteIp)) {
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            String firstForwardedIp = firstForwardedIp(forwardedFor);
            if (StringUtils.hasText(firstForwardedIp)) {
                return firstForwardedIp;
            }
            String realIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
            if (StringUtils.hasText(realIp)) {
                return realIp.trim();
            }
        }
        return remoteIp;
    }

    boolean isTrustedProxy(String remoteIp) {
        if (!StringUtils.hasText(remoteIp)) {
            return false;
        }
        return properties.getTrustedProxies().stream()
                .anyMatch(trustedProxy -> matchesTrustedProxy(remoteIp, trustedProxy));
    }

    /**
     * 读取转发头中的第一个 IP：通常代表最原始的客户端地址。
     */
    private String firstForwardedIp(String forwardedFor) {
        if (!StringUtils.hasText(forwardedFor)) {
            return null;
        }
        for (String part : forwardedFor.split(",")) {
            String candidate = part.trim();
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 判断远程 IP 是否可信代理：支持精确 IP 和 CIDR 网段。
     */
    private boolean matchesTrustedProxy(String remoteIp, String trustedProxy) {
        if (!StringUtils.hasText(trustedProxy)) {
            return false;
        }
        String trimmed = trustedProxy.trim();
        if (remoteIp.equals(trimmed)) {
            return true;
        }
        if (trimmed.contains("/")) {
            return matchesCidr(remoteIp, trimmed);
        }
        return false;
    }

    /**
     * 判断 IP 是否落在 CIDR 网段内。
     */
    private boolean matchesCidr(String remoteIp, String cidr) {
        String[] parts = cidr.split("/", 2);
        if (parts.length != 2 || !remoteIp.contains(".")) {
            return false;
        }
        try {
            int prefix = Integer.parseInt(parts[1]);
            if (prefix < 0 || prefix > 32) {
                return false;
            }
            int remote = ipv4ToInt(remoteIp);
            int network = ipv4ToInt(parts[0]);
            int mask = prefix == 0 ? 0 : -1 << (32 - prefix);
            return (remote & mask) == (network & mask);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 把 IPv4 字符串转换成整数：方便做网段匹配计算。
     */
    private int ipv4ToInt(String ip) {
        try {
            byte[] bytes = InetAddress.getByName(ip).getAddress();
            if (bytes.length != 4) {
                throw new IllegalArgumentException("not ipv4");
            }
            return ((bytes[0] & 0xff) << 24)
                    | ((bytes[1] & 0xff) << 16)
                    | ((bytes[2] & 0xff) << 8)
                    | (bytes[3] & 0xff);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("invalid ip", e);
        }
    }

    /**
     * 从请求连接信息中读取远程地址。
     */
    private String resolveRemoteAddress(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        if (remoteAddress != null && StringUtils.hasText(remoteAddress.getHostString())) {
            return remoteAddress.getHostString();
        }
        return "unknown";
    }
}
