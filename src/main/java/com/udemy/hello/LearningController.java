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
import com.udemy.hello.security.JwtAuthInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin(origins = "${frontend.origin}")
public class LearningController {

    // カテゴリー・タグは全ユーザー共有のため、編集・削除は管理者（id=1）のみ許可する
    private static final int ADMIN_USER_ID = 1;

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
    public void learning_insert(@RequestBody Learning learning, HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        learning.setUser_id(userId);

        learningService.learning_insert(learning);
        int learning_id = learningService.learning_one_select(userId);

        // タグの存在確認と挿入
        for (String name : learning.getTags()) {
            if (!learningService.tag_list().stream().anyMatch(tags -> tags.getName().equals(name))) {
                learningService.tags_insert(name);
            }
        }

        ArrayList<Integer> tags_id = new ArrayList<>();
        for (String name : learning.getTags()) {
            tags_id.add(learningService.tags_search(name));
        }

        for (Integer id : tags_id) {
            learningService.learning_tag_insert(learning_id, id);
        }
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

        // タグの存在確認と挿入
        for (String name : learning.getTags()) {
            if (!learningService.tag_list().stream().anyMatch(tags -> tags.getName().equals(name))) {
                learningService.tags_insert(name);
            }
        }

        ArrayList<Integer> tags_id = new ArrayList<>();
        for (String name : learning.getTags()) {
            tags_id.add(learningService.tags_search(name));
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

    // カテゴリの登録
    @PostMapping("/category_insert")
    public void category_insert(@RequestBody tags tag){
        learningService.category_insert(tag.getName());
    }

    // カテゴリ一覧取得
    @GetMapping("/category_list")
    public List<categories> category_list(){
        return learningService.category_list();
    }

    // カテゴリの名前変更（カテゴリーは全ユーザー共有のため、管理者のみ許可）
    @PostMapping("/category_update/{id}")
    public ResponseEntity<String> category_update(@PathVariable("id") int id, @RequestBody tags tag, HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        if (userId != ADMIN_USER_ID) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("カテゴリーの編集は管理者のみ行えます。");
        }
        learningService.category_update(id, tag.getName());
        return ResponseEntity.ok("updated");
    }

    // カテゴリの削除（使用中なら削除させない。管理者のみ許可）
    @PostMapping("/category_delete/{id}")
    public ResponseEntity<String> category_delete(@PathVariable("id") int id, HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        if (userId != ADMIN_USER_ID) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("カテゴリーの削除は管理者のみ行えます。");
        }
        int usage = learningService.category_usage_count(id);
        if (usage > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("このカテゴリーは" + usage + "件の学習記録で使用中のため削除できません。");
        }
        learningService.category_delete(id);
        return ResponseEntity.ok("deleted");
    }

    // タグの登録（重複時は何もしない）
    @PostMapping("/tag_insert")
    public void tag_insert(@RequestBody tags tag){
        boolean exists = learningService.tag_list().stream()
            .anyMatch(t -> t.getName().equals(tag.getName()));
        if (!exists) {
            learningService.tag_insert(tag.getName());
        }
    }

    // タグの名前変更（タグは全ユーザー共有のため、管理者のみ許可）
    @PostMapping("/tag_update/{id}")
    public ResponseEntity<String> tag_update(@PathVariable("id") int id, @RequestBody tags tag, HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        if (userId != ADMIN_USER_ID) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("タグの編集は管理者のみ行えます。");
        }
        learningService.tag_update(id, tag.getName());
        return ResponseEntity.ok("updated");
    }

    // タグの削除（使用中なら削除させない。管理者のみ許可）
    @PostMapping("/tag_delete/{id}")
    public ResponseEntity<String> tag_delete(@PathVariable("id") int id, HttpServletRequest request){
        int userId = JwtAuthInterceptor.getVerifiedUserId(request);
        if (userId != ADMIN_USER_ID) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("タグの削除は管理者のみ行えます。");
        }
        int usage = learningService.tag_usage_count(id);
        if (usage > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("このタグは" + usage + "件の学習記録で使用中のため削除できません。");
        }
        learningService.tag_delete(id);
        return ResponseEntity.ok("deleted");
    }

    // タグ一覧取得
    @GetMapping("/tag_list")
    public List<tags> tag_list(){
        return learningService.tag_list();
    }

    // 学習タグ一覧取得
    @GetMapping("/learning_tag_list")
    public List<learning_tag> learning_tag(){
        return learningService.learning_tag();
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
}
