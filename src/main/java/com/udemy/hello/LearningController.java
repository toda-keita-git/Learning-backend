package com.udemy.hello;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.udemy.hello.mapper.LearningService;
import com.udemy.hello.model.Learning;
import com.udemy.hello.model.categories;
import com.udemy.hello.model.tags;
import com.udemy.hello.model.learning_tag;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class LearningController {

    private final LearningApplication learningApplication;
    
    @Autowired
    LearningService learningService;

    LearningController(LearningApplication learningApplication) {
        this.learningApplication = learningApplication;
    }
    
    // ユーザーIDを指定して学習情報を取得
    @GetMapping("/learning")
    public List<Learning> findALL(@RequestParam("user_id") int user_id){
        return learningService.findALL(user_id);
    }
    
    // 学習情報の登録
    @PostMapping("/learning_insert")
    public void learning_insert(@RequestBody Learning learning){
        // user_idが必ずセットされている前提
        learningService.learning_insert(learning);
        int learning_id = learningService.learning_one_select(learning.getUser_id());

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
    
    // 学習情報の更新
    @PostMapping("/learning_update/{learning_Id}")
    public void learning_update(@RequestBody Learning learning){
        learningService.learning_update(learning);
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
    }
    
    // 学習情報の削除
    @PostMapping("/learning_delete/{id}")
    public void learning_delete(@PathVariable("id") int id){
        learningService.learning_delete(id);
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

    // カテゴリの名前変更
    @PostMapping("/category_update/{id}")
    public void category_update(@PathVariable("id") int id, @RequestBody tags tag){
        learningService.category_update(id, tag.getName());
    }

    // カテゴリの削除（使用中なら削除させない）
    @PostMapping("/category_delete/{id}")
    public ResponseEntity<String> category_delete(@PathVariable("id") int id){
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

    // タグの名前変更
    @PostMapping("/tag_update/{id}")
    public void tag_update(@PathVariable("id") int id, @RequestBody tags tag){
        learningService.tag_update(id, tag.getName());
    }

    // タグの削除（使用中なら削除させない）
    @PostMapping("/tag_delete/{id}")
    public ResponseEntity<String> tag_delete(@PathVariable("id") int id){
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
}
