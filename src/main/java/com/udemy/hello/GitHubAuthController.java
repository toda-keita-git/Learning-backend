package com.udemy.hello;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.udemy.hello.mapper.GitHubAuthService;
import java.util.Map;

@RestController
@CrossOrigin(origins = "https://learning-frontend-x5jf.onrender.com")
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
}
