package com.udemy.hello;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import com.udemy.hello.mapper.GitHubAuthService;
import com.udemy.hello.security.JwtAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@CrossOrigin(origins = "${frontend.origin}")
@RequestMapping("/github")
public class GitHubAuthController {

    @Autowired
    private GitHubAuthService gitHubAuthService;

    /**
     * GitHub OAuth コールバックを受け取り、
     * ユーザー情報をDBに保存・更新してアクセストークンを返却
     */
    @PostMapping("/callback")
    public Map<String, Object> handleGitHubCallback(@RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        Map<String, Object> result = gitHubAuthService.getAccessTokenAndRegisterUser(code);
        return result;
    }

    /**
     * ログイン中のアカウントにGitHubアカウントを連携する。
     * 新規ユーザーは作らず、既存の行にGitHub側の情報を書き足すだけなので、
     * これまでの目標・プラン・メモはそのまま保持される。
     * app_token（JWT）による本人確認が必須（誰に連携するのかをトークンで決めるため、
     * WebConfigの除外は /github/** ではなく /github/callback だけに絞ってある）。
     */
    @PostMapping("/link")
    public Map<String, Object> linkGithubAccount(HttpServletRequest request, @RequestBody Map<String, String> payload) {
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        return gitHubAuthService.linkGithubAccount(userId, payload.get("code"));
    }
}
