package com.udemy.hello.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.udemy.hello.mapper.UserMapper;
import com.udemy.hello.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class GitHubAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubAuthService.class);

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Autowired
    private UserMapper userMapper; // MyBatis Mapper

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * GitHub認証コードを使ってアクセストークン取得＋ユーザー登録・更新＋リポジトリ作成
     */
    public Map<String, Object> getAccessTokenAndRegisterUser(String code) {
        try {
            // ====== ① access_token を取得 ======
            String accessToken = getAccessToken(code);

            // ====== ② GitHubユーザー情報取得 ======
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            userHeaders.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            ResponseEntity<JsonNode> userResponse = restTemplate.exchange(
                    "https://api.github.com/user", HttpMethod.GET, new HttpEntity<>(userHeaders), JsonNode.class);

            JsonNode userJson = userResponse.getBody();
            if (userJson == null || !userJson.has("login")) {
                throw new RuntimeException("GitHubユーザー情報の取得に失敗しました。");
            }

            String githubLogin = userJson.get("login").asText();
            String email = userJson.hasNonNull("email") ? userJson.get("email").asText() : null;
            String avatarUrl = userJson.hasNonNull("avatar_url") ? userJson.get("avatar_url").asText() : null;

            // ====== ③ DB登録・更新 ======
            User existingUser = userMapper.findByGithubLogin(githubLogin);

            if (existingUser == null) {
                // --- 新規登録 ---
                User newUser = new User();
                newUser.setGithubLogin(githubLogin);
                newUser.setEmail(email);
                newUser.setAvatarUrl(avatarUrl);
                newUser.setAccessToken(accessToken);
                newUser.setCreatedRepo(false);
                userMapper.insert(newUser);

                // --- 個別リポジトリ作成 ---
                createUserRepoIfNotExist(accessToken, githubLogin);
                newUser.setCreatedRepo(true);
                userMapper.update(newUser);

                logger.info("✅ 新規ユーザー '{}' を登録し、リポジトリを作成しました。", githubLogin);

            } else {
                // --- 既存ユーザー更新 ---
                existingUser.setEmail(email);
                existingUser.setAvatarUrl(avatarUrl);
                existingUser.setAccessToken(accessToken);
                userMapper.update(existingUser);

                logger.info("ℹ️ 既存ユーザー '{}' の情報を更新しました。", githubLogin);

                // --- 未作成ならリポジトリ作成 ---
                if (!Boolean.TRUE.equals(existingUser.getCreatedRepo())) {
                    createUserRepoIfNotExist(accessToken, githubLogin);
                    existingUser.setCreatedRepo(true);
                    userMapper.update(existingUser);
                    logger.info("✅ ユーザー '{}' のリポジトリを新規作成しました。", githubLogin);
                }
            }

            // ====== ④ レスポンス返却 ======
            Map<String, Object> result = new HashMap<>();
            result.put("access_token", accessToken);
            result.put("github_login", githubLogin);
            result.put("email", email);
            result.put("avatar_url", avatarUrl);
            result.put("user_id", existingUser != null ? existingUser.getId() : newUser.getId());

            return result;

        } catch (Exception e) {
            logger.error("GitHub認証処理に失敗しました: {}", e.getMessage(), e);
            throw new RuntimeException("GitHub認証処理に失敗しました: " + e.getMessage());
        }
    }

    /**
     * GitHubにユーザー専用リポジトリを作成（既存ならスキップ）
     */
    private void createUserRepoIfNotExist(String token, String login) {
        String repoName = "learning-site-" + login;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "name", repoName,
                "private", true
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity("https://api.github.com/user/repos", entity, String.class);
            logger.info("✅ Repository '{}' created successfully for {}", repoName, login);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                logger.info("ℹ️ Repository '{}' already exists for {}", repoName, login);
            } else {
                throw e;
            }
        }
    }

    /**
     * GitHub OAuth コード → アクセストークン変換
     */
    private String getAccessToken(String code) {
        String url = "https://github.com/login/oauth/access_token";

        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(params, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<>() {}
        );

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new RuntimeException("アクセストークンの取得に失敗しました（レスポンスが空）。");
        }

        if (body.containsKey("access_token") && body.get("access_token") != null) {
            String token = (String) body.get("access_token");
            logger.info("✅ Access token successfully retrieved.");
            return token;
        } else {
            String error = (String) body.get("error");
            String errorDescription = (String) body.get("error_description");
            throw new RuntimeException("アクセストークン取得失敗: " + errorDescription + " (" + error + ")");
        }
    }
}
