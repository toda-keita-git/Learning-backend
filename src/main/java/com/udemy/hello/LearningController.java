package com.udemy.hello;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.udemy.hello.mapper.LearningService;
import com.udemy.hello.mapper.GitHubAuthService;
import com.udemy.hello.model.categories;
import com.udemy.hello.model.tags;
import com.udemy.hello.model.PlanInterest;
import com.udemy.hello.model.Inquiry;
import com.udemy.hello.security.JwtAuthInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 目標・アクションプラン・メモに共通する周辺機能（カテゴリー／タグ、GitHub連携先リポジトリの切り替え、
 * Proプラン導線、お問い合わせ）を扱うコントローラー。目標達成アプリ本体のCRUDは
 * GoalController / ActionPlanController / NoteController を参照。
 */
@Slf4j
@RestController
@CrossOrigin(origins = "${frontend.origin}")
public class LearningController {

    // お問い合わせ管理など、管理者専用の機能でのみ使用する
    private static final int ADMIN_USER_ID = 1;

    // 登録できる件数の上限（DBの肥大化を防ぐための制限。既存のカテゴリー・タグは削除されず、新規作成だけがブロックされる）。
    // 変更する場合は、利用者に案内しているPricingPlanDialog.tsxのLIMITSも必ず合わせること
    private static final int FREE_CATEGORY_LIMIT = 20;

    private final LearningApplication learningApplication;

    @Autowired
    LearningService learningService;

    @Autowired
    GitHubAuthService gitHubAuthService;

    LearningController(LearningApplication learningApplication) {
        this.learningApplication = learningApplication;
    }

    // メモの添付先として使うリポジトリを、本人の既存リポジトリに切り替える
    // （新規作成は行わない。利用者の特定にはJWTで検証済みのuser_idを使う。
    //   github_loginクレームはGoogleログイン時にメールアドレスが入るため使えない）
    @PostMapping("/user_repo_select")
    public ResponseEntity<?> userRepoSelect(@RequestBody Map<String, String> body, HttpServletRequest request) {
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        String repoName = body.get("repo_name");
        if (repoName == null || repoName.isBlank()) {
            return ResponseEntity.badRequest().body("リポジトリ名を指定してください。");
        }
        String updated = gitHubAuthService.updateUserRepo(userId, repoName.trim());
        return ResponseEntity.ok(Map.of("repo_name", updated));
    }

    // カテゴリの登録（本人専用のカテゴリーとして登録される）
    @PostMapping("/category_insert")
    public ResponseEntity<String> category_insert(@RequestBody tags tag, HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        // フリープランの上限チェック（既存のカテゴリーは消さず、新規作成だけをブロックする）
        if (learningService.category_count(userId) >= FREE_CATEGORY_LIMIT) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("カテゴリーの登録上限（" + FREE_CATEGORY_LIMIT + "件）に達しています。使っていないカテゴリーを削除すると、新しく追加できます。");
        }
        learningService.category_insert(tag.getName(), userId);
        return ResponseEntity.ok("inserted");
    }

    // カテゴリ一覧取得（本人のものだけ）
    @GetMapping("/category_list")
    public List<categories> category_list(HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        return learningService.category_list(userId);
    }

    // カテゴリの名前変更（カテゴリーは個人ごとに管理するため、本人が作成したもの以外は編集できない）
    @PostMapping("/category_update/{id}")
    public ResponseEntity<String> category_update(@PathVariable("id") int id, @RequestBody tags tag, HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        int updated = learningService.category_update(id, tag.getName(), userId);
        if (updated == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("このカテゴリーを編集する権限がありません。");
        }
        return ResponseEntity.ok("updated");
    }

    // カテゴリの削除（使用中なら削除させない。本人が作成したもの以外は削除できない）
    @PostMapping("/category_delete/{id}")
    public ResponseEntity<String> category_delete(@PathVariable("id") int id, HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        int usage = learningService.category_usage_count(id, userId);
        if (usage > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("このカテゴリーは" + usage + "件のメモで使用中のため削除できません。");
        }
        int deleted = learningService.category_delete(id, userId);
        if (deleted == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("このカテゴリーを削除する権限がありません。");
        }
        return ResponseEntity.ok("deleted");
    }

    // タグの登録（本人専用のタグとして登録される。重複時は何もしない）
    @PostMapping("/tag_insert")
    public ResponseEntity<String> tag_insert(@RequestBody tags tag, HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        boolean exists = learningService.tag_list(userId).stream()
            .anyMatch(t -> t.getName().equals(tag.getName()));
        if (!exists) {
            // フリープランの上限チェック（既存のタグは消さず、新規作成だけをブロックする）
            if (learningService.tag_count(userId) >= LearningService.FREE_TAG_LIMIT) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("タグの登録上限（" + LearningService.FREE_TAG_LIMIT + "件）に達しています。使っていないタグを削除すると、新しく追加できます。");
            }
            learningService.tag_insert(tag.getName(), userId);
        }
        return ResponseEntity.ok("inserted");
    }

    // タグの名前変更（タグは個人ごとに管理するため、本人が作成したもの以外は編集できない）
    @PostMapping("/tag_update/{id}")
    public ResponseEntity<String> tag_update(@PathVariable("id") int id, @RequestBody tags tag, HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        int updated = learningService.tag_update(id, tag.getName(), userId);
        if (updated == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("このタグを編集する権限がありません。");
        }
        return ResponseEntity.ok("updated");
    }

    // タグの削除（使用中なら削除させない。本人が作成したもの以外は削除できない）
    @PostMapping("/tag_delete/{id}")
    public ResponseEntity<String> tag_delete(@PathVariable("id") int id, HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        int usage = learningService.tag_usage_count(id, userId);
        if (usage > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("このタグは" + usage + "件のメモで使用中のため削除できません。");
        }
        int deleted = learningService.tag_delete(id, userId);
        if (deleted == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("このタグを削除する権限がありません。");
        }
        return ResponseEntity.ok("deleted");
    }

    // タグ一覧取得（本人のものだけ）
    @GetMapping("/tag_list")
    public List<tags> tag_list(HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        return learningService.tag_list(userId);
    }

    // Proプラン「通知を希望する」の登録（同じユーザーが複数回押しても1件のみ記録される）
    @PostMapping("/plan_interest_register")
    public void plan_interest_register(HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        String githubLogin = JwtAuthInterceptor.getVerifiedGithubLogin(request);
        PlanInterest planInterest = new PlanInterest();
        planInterest.setUser_id(userId);
        planInterest.setGithub_login(githubLogin);
        learningService.plan_interest_insert(planInterest);
    }

    // 自分が既に「通知を希望する」を押しているか
    @GetMapping("/plan_interest_check")
    public Map<String, Boolean> plan_interest_check(HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        return Map.of("requested", learningService.plan_interest_exists(userId));
    }

    // お問い合わせの送信（未ログインの一般訪問者も送れるよう認証不要。WebConfigで/inquiry_submitを除外している）
    @PostMapping("/inquiry_submit")
    public ResponseEntity<String> inquiry_submit(@RequestBody Inquiry inquiry){
        if (inquiry.getEmail() == null || inquiry.getEmail().isBlank()
                || inquiry.getMessage() == null || inquiry.getMessage().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("メールアドレスとお問い合わせ内容は必須です。");
        }
        learningService.inquiry_insert(inquiry);
        return ResponseEntity.ok("submitted");
    }

    // お問い合わせ一覧の取得（管理者のみ）
    @GetMapping("/inquiry_list")
    public ResponseEntity<List<Inquiry>> inquiry_list(HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        if (userId != ADMIN_USER_ID) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(learningService.inquiry_list());
    }

    // お問い合わせのステータス更新（管理者のみ。new→read→done）
    @PostMapping("/inquiry_status/{id}")
    public ResponseEntity<String> inquiry_status_update(@PathVariable("id") int id, @RequestBody Map<String, String> body, HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        if (userId != ADMIN_USER_ID) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("お問い合わせの管理は管理者のみ行えます。");
        }
        learningService.inquiry_status_update(id, body.get("status"));
        return ResponseEntity.ok("updated");
    }
}
