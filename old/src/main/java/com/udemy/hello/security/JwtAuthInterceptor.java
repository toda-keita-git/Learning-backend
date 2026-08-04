package com.udemy.hello.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * リクエストのAuthorizationヘッダーにあるJWTを検証し、
 * 本人確認済みのuser_idをリクエスト属性にセットする。
 * トークンが無い・無効・期限切れの場合は401を返す。
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String IDENTITY_ATTR = "verifiedIdentity";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtService jwtService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // CORSのプリフライト(OPTIONS)にはAuthorizationヘッダーが付かない仕様のため、
        // ここで弾くとブラウザからのクロスオリジンリクエストが全て失敗してしまう
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "認証トークンがありません。再度ログインしてください。");
            return false;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            JwtService.VerifiedIdentity identity = jwtService.verify(token);
            request.setAttribute(IDENTITY_ATTR, identity);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "認証トークンが無効です。再度ログインしてください。");
            return false;
        }
    }

    private static JwtService.VerifiedIdentity getVerifiedIdentity(HttpServletRequest request) {
        Object value = request.getAttribute(IDENTITY_ATTR);
        if (value == null) {
            throw new IllegalStateException("verifiedIdentity is missing; JwtAuthInterceptor did not run for this request.");
        }
        return (JwtService.VerifiedIdentity) value;
    }

    /** preHandleを通過したリクエストから、検証済みのuser_idを取り出す */
    public static int getVerifiedUserId(HttpServletRequest request) {
        return getVerifiedIdentity(request).userId();
    }

    /** preHandleを通過したリクエストから、検証済みのgithub_loginを取り出す */
    public static String getVerifiedGithubLogin(HttpServletRequest request) {
        return getVerifiedIdentity(request).githubLogin();
    }
}
