package com.udemy.hello.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.udemy.hello.model.User;
import com.udemy.hello.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Googleドライブ連携（GitHub連携と並存）用の認証処理。
 * GitHubAuthServiceと同じ構造で並存させ、GitHub側の既存コードには一切手を入れない。
 *
 * GitHubとの重要な違い: Googleのアクセストークンは短命（約1時間）で失効するため、
 * refresh_tokenをDBに保存しておき、必要になるたびに再取得する。refresh_tokenは
 * フロントエンドには絶対に返さない（サーバー側だけが保持する）。
 */
@Service
public class GoogleAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthService.class);

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files";

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private LearningService learningService;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Google認証コードを使ってアクセストークン取得＋ユーザー登録・更新＋Driveフォルダ作成
     */
    public Map<String, Object> getAccessTokenAndRegisterUser(String code) {
        try {
            // ====== ① トークン取得（Googleのトークンエンドポイントはform-urlencoded必須） ======
            TokenResponse tokenResponse = exchangeCodeForTokens(code);

            // ====== ② Googleユーザー情報取得 ======
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(tokenResponse.accessToken);

            ResponseEntity<JsonNode> userResponse = restTemplate.exchange(
                    USERINFO_URL, HttpMethod.GET, new HttpEntity<>(userHeaders), JsonNode.class);

            JsonNode userJson = userResponse.getBody();
            if (userJson == null || !userJson.has("sub")) {
                throw new RuntimeException("Googleユーザー情報の取得に失敗しました。");
            }

            String sub = userJson.get("sub").asText();
            String email = userJson.hasNonNull("email") ? userJson.get("email").asText() : null;
            String avatarUrl = userJson.hasNonNull("picture") ? userJson.get("picture").asText() : null;

            // ====== ③ DB登録・更新 ======
            User existingUser = userMapper.findByGoogleSub(sub);
            User user;

            if (existingUser == null) {
                // --- 新規登録 ---
                User newUser = new User();
                newUser.setAuthProvider("google");
                newUser.setGoogleSub(sub);
                newUser.setEmail(email);
                newUser.setAvatarUrl(avatarUrl);
                newUser.setCreatedRepo(false);
                userMapper.insert(newUser);

                // --- 添付先として使うDriveフォルダを作成 ---
                String folderId = createUserDriveFolder(tokenResponse.accessToken, sub);
                newUser.setDriveFolderId(folderId);
                userMapper.updateDriveFolderId(newUser.getId(), folderId);

                // --- 初期カテゴリー・タグの用意（一般＋プログラミング関連、GitHub版と同じ） ---
                learningService.seedDefaultCategoriesAndTags(newUser.getId());

                user = newUser;
                logger.info("✅ 新規Googleユーザー '{}' を登録し、Driveフォルダを作成しました。", sub);
            } else {
                // --- 既存ユーザー更新 ---
                existingUser.setEmail(email);
                existingUser.setAvatarUrl(avatarUrl);
                userMapper.updateGoogleUser(existingUser);

                // 未作成ならDriveフォルダ作成（このカラムが追加される前からのユーザー等への保険）
                if (existingUser.getDriveFolderId() == null || existingUser.getDriveFolderId().isBlank()) {
                    String folderId = createUserDriveFolder(tokenResponse.accessToken, sub);
                    existingUser.setDriveFolderId(folderId);
                    userMapper.updateDriveFolderId(existingUser.getId(), folderId);
                }

                user = existingUser;
                logger.info("ℹ️ 既存Googleユーザー '{}' の情報を更新しました。", sub);
            }

            // refresh_tokenは初回同意時（またはprompt=consent強制時）のみ返る。
            // 返ってきた場合だけ保存し、そうでない場合は既存の値を保持する
            if (tokenResponse.refreshToken != null && !tokenResponse.refreshToken.isBlank()) {
                userMapper.updateGoogleRefreshToken(user.getId(), tokenResponse.refreshToken);
            } else if (existingUser == null) {
                logger.warn("⚠️ Google新規登録時にrefresh_tokenが返却されませんでした（prompt=consentの設定を確認してください）。");
            }

            // ====== ④ レスポンス返却（refresh_tokenは絶対に含めない） ======
            Map<String, Object> result = new HashMap<>();
            result.put("access_token", tokenResponse.accessToken);
            result.put("expires_in", tokenResponse.expiresIn);
            result.put("email", email);
            result.put("avatar_url", avatarUrl);
            result.put("user_id", user.getId());
            result.put("provider", "google");
            result.put("drive_folder_id", user.getDriveFolderId());
            // 以降のAPI呼び出しの本人確認に使う、こちらが署名したトークン。
            // JwtServiceのクレーム名はgithub_loginのままだが、Googleユーザーの場合は
            // メールアドレスを詰めて流用する（表示・記録用途のみで、GitHub API呼び出しには使わない）
            result.put("app_token", jwtService.issueToken(user.getId(), email));

            return result;

        } catch (Exception e) {
            logger.error("Google認証処理に失敗しました: {}", e.getMessage(), e);
            throw new RuntimeException("Google認証処理に失敗しました: " + e.getMessage());
        }
    }

    /**
     * 保存済みのrefresh_tokenを使って、短命なDriveアクセストークンを再取得する。
     * フロントエンドはDriveの操作が必要になるたびにこれを呼ぶ想定
     * （/google/refresh。JWT認証必須のエンドポイント経由でuserIdを特定してから呼ばれる）。
     */
    public Map<String, Object> refreshAccessToken(int userId) {
        User user = userMapper.findById(userId);
        if (user == null || user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isBlank()) {
            throw new RuntimeException("Google連携が完了していません。再度Googleでログインしてください。");
        }

        TokenResponse tokenResponse = refreshWithToken(user.getGoogleRefreshToken());

        Map<String, Object> result = new HashMap<>();
        result.put("access_token", tokenResponse.accessToken);
        result.put("expires_in", tokenResponse.expiresIn);
        return result;
    }

    private String createUserDriveFolder(String accessToken, String sub) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "learning-site-" + sub.substring(0, Math.min(8, sub.length())));
        body.put("mimeType", "application/vnd.google-apps.folder");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(DRIVE_FILES_URL, entity, JsonNode.class);

        JsonNode responseBody = response.getBody();
        if (responseBody == null || !responseBody.has("id")) {
            throw new RuntimeException("Googleドライブのフォルダ作成に失敗しました。");
        }
        return responseBody.get("id").asText();
    }

    /**
     * Google OAuth コード（または refresh_token）→ アクセストークン変換。
     * GoogleのトークンエンドポイントはApplication/x-www-form-urlencoded必須で、
     * GitHubのエンドポイントと異なりJSON bodyを受け付けない点に注意。
     */
    private TokenResponse exchangeCodeForTokens(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        return postToTokenEndpoint(params);
    }

    private TokenResponse refreshWithToken(String refreshToken) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", refreshToken);
        params.add("grant_type", "refresh_token");

        return postToTokenEndpoint(params);
    }

    private TokenResponse postToTokenEndpoint(MultiValueMap<String, String> params) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(TOKEN_URL, requestEntity, JsonNode.class);

        JsonNode body = response.getBody();
        if (body == null || !body.has("access_token")) {
            throw new RuntimeException("Googleのアクセストークン取得に失敗しました。");
        }

        TokenResponse result = new TokenResponse();
        result.accessToken = body.get("access_token").asText();
        result.expiresIn = body.hasNonNull("expires_in") ? body.get("expires_in").asInt() : 3600;
        result.refreshToken = body.hasNonNull("refresh_token") ? body.get("refresh_token").asText() : null;
        return result;
    }

    private static class TokenResponse {
        String accessToken;
        int expiresIn;
        String refreshToken;
    }
}
