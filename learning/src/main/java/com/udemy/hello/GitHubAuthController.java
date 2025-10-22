package com.udemy.hello;

import org.springframework.core.ParameterizedTypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * GitHub認証からのコールバックを処理するコントローラー
 */
@RestController
@RequestMapping("/github")
public class GitHubAuthController {

    @Autowired
    private GitHubAuthService gitHubAuthService;

    /**
     * フロントエンドからのコールバックを受け取り、アクセストークンを返す
     * @param code GitHubから発行された認証コード
     * @return アクセストークンを含むJSON
     */
    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping
    public Map<String, String> githubCallback(@RequestParam("code") String code) {
        String accessToken = gitHubAuthService.getAccessToken(code);
        return Collections.singletonMap("token", accessToken);
    }
}

/**
 * GitHub APIと通信してアクセストークンを取得するサービス
 */
@Service
class GitHubAuthService {

    // ロガーを追加してレスポンス内容を確認できるようにする
    private static final Logger logger = LoggerFactory.getLogger(GitHubAuthService.class);

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 認証コードを使ってGitHubからアクセストークンを取得する
     * @param code 認証コード
     * @return アクセストークン
     */
    public String getAccessToken(String code) {
        String url = "https://github.com/login/oauth/access_token";

        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(params, headers);

        // レスポンスの型を Map<String, Object> に指定
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<>() {} // 型情報を渡す
        );

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new RuntimeException("アクセストークンの取得に失敗しました (レスポンスが空です)。");
        }
        
        // レスポンス内容をログに出力してデバッグしやすくする
        logger.info("GitHub API Response: {}", body);

        // access_token が存在するかチェック
        if (body.containsKey("access_token") && body.get("access_token") != null) {
            return (String) body.get("access_token");
        } else {
            // エラーの場合、詳細なエラーメッセージを例外に含める
            String error = (String) body.get("error");
            String errorDescription = (String) body.get("error_description");
            throw new RuntimeException("アクセストークンの取得に失敗しました。理由: " + errorDescription + " (エラー: " + error + ")");
        }
    }
}