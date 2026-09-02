package com.udemy.hello;

import com.udemy.hello.mapper.AccountService;
import com.udemy.hello.mapper.GoogleAuthService;
import com.udemy.hello.mapper.UserMapper;
import com.udemy.hello.model.User;
import com.udemy.hello.security.JwtAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ログイン中アカウントの情報を返す。
 * GitHubとGoogleのどちらが連携済みかをフロントエンドが知るために使う
 * （どちらか一方しか連携していなければ「連携する」ボタンを出す）。
 */
@RestController
@CrossOrigin(origins = "${frontend.origin}")
public class AccountController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GoogleAuthService googleAuthService;

    @Autowired
    private AccountService accountService;

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("ユーザーが見つかりません。");
        }

        boolean hasGithub = user.getGithubLogin() != null && !user.getGithubLogin().isBlank();
        boolean hasGoogle = user.getGoogleSub() != null && !user.getGoogleSub().isBlank();

        Map<String, Object> result = new HashMap<>();
        result.put("user_id", user.getId());
        result.put("email", user.getEmail());
        result.put("avatar_url", user.getAvatarUrl());
        result.put("auth_provider", user.getAuthProvider());
        // 連携済みかどうか。トークン等の秘密情報は返さない
        result.put("has_github", hasGithub);
        result.put("has_google", hasGoogle);
        result.put("github_login", user.getGithubLogin());
        result.put("repo_name", user.getRepoName());
        // Googleは連携済みなのにフォルダIDだけ欠けていると、メモの編集画面で
        // 「保存先: Google」が選べないままになる。ここで気付いた時点で作り直す
        String driveFolderId = hasGoogle ? googleAuthService.ensureDriveFolderId(user) : user.getDriveFolderId();
        result.put("drive_folder_id", driveFolderId);
        return result;
    }

    // 「アカウントデータの削除」。目標・プラン・メモを全削除し、アカウント自体（ログイン・連携情報）は残す。
    // 論理削除のため、間違えて押した場合の物理的な巻き戻しはできないが、DB操作自体は取り消し可能な形にしてある
    @PostMapping("/account_data_delete")
    public ResponseEntity<String> accountDataDelete(HttpServletRequest request) {
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        accountService.deleteAllUserData(userId);
        return ResponseEntity.ok("deleted");
    }
}
