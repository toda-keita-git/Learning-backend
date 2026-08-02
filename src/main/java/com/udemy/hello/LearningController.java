package com.udemy.hello;

import java.util.ArrayList;
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
import com.udemy.hello.model.Learning;
import com.udemy.hello.model.categories;
import com.udemy.hello.model.tags;
import com.udemy.hello.model.learning_tag;
import com.udemy.hello.model.PlanInterest;
import com.udemy.hello.model.Inquiry;
import com.udemy.hello.security.JwtAuthInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin(origins = "${frontend.origin}")
public class LearningController {

    // お問い合わせ管理など、管理者専用の機能でのみ使用する
    private static final int ADMIN_USER_ID = 1;

    // フリープランの登録上限（Proプランとの差別化のための制限。既存の記録は上限を
    // 超えていても削除されず、新規登録だけがブロックされる）
    private static final int FREE_PLAN_LIMIT = 100;
    // カテゴリー・タグも同様に新規作成のみを上限でブロックする
    private static final int FREE_CATEGORY_LIMIT = 20;
    private static final int FREE_TAG_LIMIT = 50;

    private final LearningApplication learningApplication;

    @Autowired
    LearningService learningService;

    LearningController(LearningApplication learningApplication) {
        this.learningApplication = learningApplication;
    }

    // ログイン中の本人の学習情報を取得（user_idはJWTで検証済みのものを使う。クライアントの自己申告は信用しない）
    @GetMapping("/learning")
    public List<Learning> findALL(HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        return learningService.findALL(userId);
    }

    // 学習情報の登録（user_idはJWTで検証済みの本人のものを強制的に使う）
    @PostMapping("/learning_insert")
    public ResponseEntity<String> learning_insert(@RequestBody Learning learning, HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        learning.setUser_id(userId);

        // フリープランの上限チェック（既存の記録は消さず、新規登録だけをブロックする）
        if (learningService.learning_count(userId) >= FREE_PLAN_LIMIT) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("フリープランの登録上限（" + FREE_PLAN_LIMIT + "件）に達しています。Proプランのご案内をご確認ください。");
        }

        learningService.learning_insert(learning);
        int learning_id = learningService.learning_one_select(userId);

        // タグの存在確認と挿入（タグは個人ごとに管理するため、本人のタグの中だけで確認する。
        // フリープランのタグ上限に達している場合、新規タグとしては作らずスキップする
        // （記録自体の保存は続行し、上限超過分のタグだけが付かない形になる）
        for (String name : learning.getTags()) {
            boolean exists = learningService.tag_list(userId).stream().anyMatch(tags -> tags.getName().equals(name));
            if (!exists && learningService.tag_count(userId) < FREE_TAG_LIMIT) {
                learningService.tags_insert(name, userId);
            }
        }

        // 上限超過でスキップされた名前はtags_searchがnullを返すため、紐づけ対象から除外する
        ArrayList<Integer> tags_id = new ArrayList<>();
        for (String name : learning.getTags()) {
            Integer tagId = learningService.tags_search(name, userId);
            if (tagId != null) {
                tags_id.add(tagId);
            }
        }

        for (Integer id : tags_id) {
            learningService.learning_tag_insert(learning_id, id);
        }

        return ResponseEntity.ok("inserted");
    }

    // 学習情報の更新（本人が作成した記録以外は更新できない）
    @PostMapping("/learning_update/{learning_Id}")
    public ResponseEntity<String> learning_update(@RequestBody Learning learning, HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        learning.setUser_id(userId);

        int updated = learningService.learning_update(learning);
        if (updated == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("この学習記録を編集する権限がありません。");
        }

        int learning_id = learning.getId();

        // タグの存在確認と挿入（タグは個人ごとに管理するため、本人のタグの中だけで確認する。
        // フリープランのタグ上限に達している場合、新規タグとしては作らずスキップする
        // （記録自体の保存は続行し、上限超過分のタグだけが付かない形になる）
        for (String name : learning.getTags()) {
            boolean exists = learningService.tag_list(userId).stream().anyMatch(tags -> tags.getName().equals(name));
            if (!exists && learningService.tag_count(userId) < FREE_TAG_LIMIT) {
                learningService.tags_insert(name, userId);
            }
        }

        // 上限超過でスキップされた名前はtags_searchがnullを返すため、紐づけ対象から除外する
        ArrayList<Integer> tags_id = new ArrayList<>();
        for (String name : learning.getTags()) {
            Integer tagId = learningService.tags_search(name, userId);
            if (tagId != null) {
                tags_id.add(tagId);
            }
        }

        // 既存タグを削除して更新
        learningService.tags_delete(learning_id);
        for (Integer id : tags_id) {
            learningService.learning_tag_insert(learning_id, id);
        }

        return ResponseEntity.ok("updated");
    }

    // 学習情報の削除（本人が作成した記録以外は削除できない）
    @PostMapping("/learning_delete/{id}")
    public ResponseEntity<String> learning_delete(@PathVariable("id") int id, HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        int deleted = learningService.learning_delete(id, userId);
        if (deleted == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("この学習記録を削除する権限がありません。");
        }
        return ResponseEntity.ok("deleted");
    }

    // カテゴリの登録（本人専用のカテゴリーとして登録される）
    @PostMapping("/category_insert")
    public ResponseEntity<String> category_insert(@RequestBody tags tag, HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        // フリープランの上限チェック（既存のカテゴリーは消さず、新規作成だけをブロックする）
        if (learningService.category_count(userId) >= FREE_CATEGORY_LIMIT) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("フリープランのカテゴリー上限（" + FREE_CATEGORY_LIMIT + "件）に達しています。Proプランのご案内をご確認ください。");
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
                .body("このカテゴリーは" + usage + "件の学習記録で使用中のため削除できません。");
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
            if (learningService.tag_count(userId) >= FREE_TAG_LIMIT) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("フリープランのタグ上限（" + FREE_TAG_LIMIT + "件）に達しています。Proプランのご案内をご確認ください。");
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
                .body("このタグは" + usage + "件の学習記録で使用中のため削除できません。");
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

    // 学習タグ一覧取得（本人の学習記録に紐づくものだけ）
    @GetMapping("/learning_tag_list")
    public List<learning_tag> learning_tag(HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        return learningService.learning_tag(userId);
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
