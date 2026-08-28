package com.udemy.hello;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.udemy.hello.mapper.GoogleAuthService;
import com.udemy.hello.security.JwtAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@CrossOrigin(origins = "${frontend.origin}")
@RequestMapping("/google")
public class GoogleAuthController {

    @Autowired
    private GoogleAuthService googleAuthService;

    /**
     * Google OAuth コールバックを受け取り、
     * ユーザー情報をDBに保存・更新してアクセストークンを返却
     * （JwtAuthInterceptorの対象外エンドポイント。まだapp_tokenを持っていないため）
     */
    @PostMapping("/callback")
    public Map<String, Object> handleGoogleCallback(@RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        return googleAuthService.getAccessTokenAndRegisterUser(code);
    }

    /**
     * 短命なDriveアクセストークンを再取得する。
     * app_token（JWT）による本人確認が必須のエンドポイント
     * （WebConfigのexcludePathPatternsに含めていないため、未認証では呼べない）。
     */
    @PostMapping("/refresh")
    public Map<String, Object> refreshAccessToken(HttpServletRequest request) {
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        return googleAuthService.refreshAccessToken(userId);
    }

    /**
     * ログイン中のアカウントにGoogleアカウントを連携する。
     * 新規ユーザーは作らず、既存の行にGoogle側の情報を書き足すだけなので、
     * これまでの目標・プラン・メモはそのまま保持される。
     * app_token（JWT）による本人確認が必須（誰に連携するのかをトークンで決めるため）。
     */
    @PostMapping("/link")
    public Map<String, Object> linkGoogleAccount(HttpServletRequest request, @RequestBody Map<String, String> payload) {
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        return googleAuthService.linkGoogleAccount(userId, payload.get("code"));
    }
}
