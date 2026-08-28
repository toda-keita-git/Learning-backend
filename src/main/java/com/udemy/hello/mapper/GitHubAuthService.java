package com.udemy.hello.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.udemy.hello.mapper.UserMapper;
import com.udemy.hello.model.User;
import com.udemy.hello.security.JwtService;
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

    @Autowired
    private JwtService jwtService;

    @Autowired
    private LearningService learningService;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * GitHub認証コードを使ってアクセストークン取得＋ユーザー登録・更新＋リポジトリ作成
     */
    public Map<String, Object> getAccessTokenAndRegisterUser(String code) {
        try {
            User newUser = new User();

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
                newUser.setGithubLogin(githubLogin);
                newUser.setEmail(email);
                newUser.setAvatarUrl(avatarUrl);
                newUser.setAccessToken(accessToken);
                newUser.setCreatedRepo(false);
                // 明示的にNULLを渡すとDBのDEFAULT句が効かずauth_providerがNULLのまま
                // 入ってしまうため、GitHub経路であることを明示しておく
                newUser.setAuthProvider("github");
                userMapper.insert(newUser);

                // --- 個別リポジトリ作成 ---
                createUserRepoIfNotExist(accessToken, githubLogin);
                newUser.setCreatedRepo(true);
                userMapper.update(newUser);
                // 作成したリポジトリを、以降の添付先として使うリポジトリに設定する
                newUser.setRepoName("learning-site-" + githubLogin);
                userMapper.updateRepoName(newUser.getId(), newUser.getRepoName());

                // --- 初期カテゴリー・タグの用意（一般＋プログラミング関連） ---
                learningService.seedDefaultCategoriesAndTags(newUser.getId());

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
                // repo_name が未設定（このカラムが追加される前からのユーザー）なら、
                // これまで使っていた既定のリポジトリ名で埋めておく
                if (existingUser.getRepoName() == null || existingUser.getRepoName().isBlank()) {
                    existingUser.setRepoName("learning-site-" + githubLogin);
                    userMapper.updateRepoName(existingUser.getId(), existingUser.getRepoName());
                }
            }

            // ====== ④ レスポンス返却 ======
            int userId = existingUser != null ? existingUser.getId() : newUser.getId();
            String repoName = existingUser != null ? existingUser.getRepoName() : newUser.getRepoName();

            Map<String, Object> result = new HashMap<>();
            result.put("access_token", accessToken);
            result.put("github_login", githubLogin);
            result.put("email", email);
            result.put("avatar_url", avatarUrl);
            result.put("user_id", userId);
            result.put("repo_name", repoName);
            // 以降のAPI呼び出しの本人確認に使う、こちらが署名したトークン。
            // access_tokenはGitHub API呼び出し専用として使い続け、
            // このアプリのAPIに対する本人確認にはapp_tokenを使う
            result.put("app_token", jwtService.issueToken(userId, githubLogin));

            return result;

        } catch (Exception e) {
            logger.error("GitHub認証処理に失敗しました: {}", e.getMessage(), e);
            throw new RuntimeException("GitHub認証処理に失敗しました: " + e.getMessage());
        }
    }

    /**
     * 保存済みのGitHubアクセストークンを返す。
     *
     * GitHubへの添付操作（リポジトリへのファイル作成・取得）はブラウザ側のOctokitが行うため、
     * トークンがブラウザに無いと操作できない。GitHubでログインした場合はログイン応答で
     * 受け取れるが、Googleでログインして後からGitHubを連携した場合は受け取る機会が無い。
     * そのためGoogleの/google/refreshと同じく、サーバーが保持している値を
     * 本人確認(JWT)のうえで渡せるようにする。
     */
    public Map<String, Object> getStoredGithubToken(int userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("ユーザーが見つかりません。");
        }
        if (user.getAccessToken() == null || user.getAccessToken().isBlank()) {
            throw new RuntimeException("GitHub連携が完了していません。GitHubアカウントを連携してください。");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("access_token", user.getAccessToken());
        result.put("github_login", user.getGithubLogin());
        result.put("repo_name", user.getRepoName());
        return result;
    }

    /**
     * ログイン中のユーザー（userId）に、GitHubアカウントを「連携」する。
     * ログインと違い新規ユーザーは作らず、既存の行にGitHub側の情報を書き足すだけ。
     * これにより、Googleで作った目標・プラン・メモをそのまま保持したまま、
     * 添付先としてGitHubリポジトリも使えるようになる。
     */
    public Map<String, Object> linkGithubAccount(int userId, String code) {
        try {
            User user = userMapper.findById(userId);
            if (user == null) {
                throw new RuntimeException("ユーザーが見つかりません。");
            }

            String accessToken = getAccessToken(code);

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

            // 同じGitHubアカウントが別ユーザーに連携済みだと、どちらの持ち物か
            // 決められなくなるため拒否する（データの取り違えを防ぐ）
            User owner = userMapper.findByGithubLogin(githubLogin);
            if (owner != null && !owner.getId().equals(user.getId())) {
                throw new RuntimeException("このGitHubアカウントは既に別のアカウントに連携されています。");
            }

            // 添付先リポジトリはまだ持っていない場合だけ用意する
            String repoName = user.getRepoName();
            boolean createdRepo = user.getCreatedRepo();
            if (repoName == null || repoName.isBlank()) {
                createUserRepoIfNotExist(accessToken, githubLogin);
                repoName = "learning-site-" + githubLogin;
                createdRepo = true;
            }

            userMapper.linkGithubAccount(user.getId(), githubLogin, accessToken, repoName, createdRepo);
            userMapper.fillProfileIfEmpty(user.getId(), email, avatarUrl);

            logger.info("✅ ユーザーID {} にGitHubアカウント '{}' を連携しました。", user.getId(), githubLogin);

            Map<String, Object> result = new HashMap<>();
            result.put("access_token", accessToken);
            result.put("github_login", githubLogin);
            result.put("repo_name", repoName);
            return result;

        } catch (Exception e) {
            logger.error("GitHubアカウントの連携に失敗しました: {}", e.getMessage(), e);
            throw new RuntimeException("GitHubアカウントの連携に失敗しました: " + e.getMessage());
        }
    }

    /**
     * 学習記録の添付先として使うリポジトリを、本人の既存リポジトリに切り替える。
     * 新規作成は行わない（自動作成される既定のリポジトリとは別に、本人がGitHub上に
     * 既に持っているリポジトリを選ぶための機能）。
     */
    public String updateUserRepo(String githubLogin, String repoName) {
        User user = userMapper.findByGithubLogin(githubLogin);
        if (user == null) {
            throw new RuntimeException("ユーザーが見つかりません。");
        }
        userMapper.updateRepoName(user.getId(), repoName);
        return repoName;
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
