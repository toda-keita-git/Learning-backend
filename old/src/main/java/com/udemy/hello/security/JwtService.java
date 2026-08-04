package com.udemy.hello.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * ログイン後の本人確認に使うトークン(JWT)の発行・検証。
 * GitHubアクセストークンとは別に、自前で署名したトークンをuser_idの
 * 検証に使うことで、リクエストごとにGitHub APIへ問い合わせずに済む。
 */
@Component
public class JwtService {

    // ログイン状態をlocalStorageへ永続化し、オフラインでも開き直せるようにしている
    // （AuthProvider参照）。有効期限を短くしすぎると、オフラインが続く間に
    // トークンが切れて再ログイン（＝通信必須）を求められ、オフライン対応の
    // 意味が薄れてしまうため、ある程度長めに設定する
    private static final Duration EXPIRATION = Duration.ofDays(30);

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(normalize(secret));
    }

    // JWT_SECRETがどんな長さの文字列でも、HS256に必要な256bit鍵として使えるよう正規化する
    private byte[] normalize(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public String issueToken(int userId, String githubLogin) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION.toMillis());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("github_login", githubLogin)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** 検証済みトークンから取り出した本人確認情報 */
    public record VerifiedIdentity(int userId, String githubLogin) {
    }

    /**
     * トークンを検証して本人確認情報を返す。
     * 署名が不正・期限切れ等の場合はio.jsonwebtoken.JwtExceptionを投げる。
     */
    public VerifiedIdentity verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        int userId = Integer.parseInt(claims.getSubject());
        String githubLogin = claims.get("github_login", String.class);
        return new VerifiedIdentity(userId, githubLogin);
    }
}
