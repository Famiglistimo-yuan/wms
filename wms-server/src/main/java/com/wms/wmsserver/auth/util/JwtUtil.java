package com.wms.wmsserver.auth.util;

import com.wms.wmsserver.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 签发与解析工具。token payload：
 *   sub = userId（Long）
 *   username = 登录用户名
 *   permissions = 权限码集合（字符串列表）
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireHours;

    public JwtUtil(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expireHours = properties.getExpireHours();
    }

    /** 签发 JWT */
    public String issue(Long userId, String username, List<String> permissions) {
        long now = System.currentTimeMillis();
        long exp = now + expireHours * 3600_000L;
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("permissions", permissions)
                .issuedAt(new Date(now))
                .expiration(new Date(exp))
                .signWith(key)
                .compact();
    }

    /** 解析 JWT，返回全部 Claims；非法/过期/篡改均抛 JwtException */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
