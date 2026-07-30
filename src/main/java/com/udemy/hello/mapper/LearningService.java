package com.udemy.hello.mapper;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.udemy.hello.model.Learning;
import com.udemy.hello.model.categories;
import com.udemy.hello.model.tags;
import com.udemy.hello.model.learning_tag;
import com.udemy.hello.model.PlanInterest;

@Service
public class LearningService {
    
    @Autowired
    private LearningMapper learningMapper;
    
    // ユーザーID指定で全学習情報取得
    public List<Learning> findALL(int user_id) {
        return learningMapper.findAll(user_id);
    }
    
    public List<categories> category_list() {
        return learningMapper.category_list();
    }
    
    public List<tags> tag_list() {
        return learningMapper.tag_list();
    }
    
    public List<learning_tag> learning_tag() {
        return learningMapper.learning_tag();
    }
    
    // 学習情報登録（user_idを含む）
    public int learning_insert(Learning learning) {
        return learningMapper.learning_insert(learning);
    }

    // 最新学習IDを取得（user_id指定）
    public Integer learning_one_select(int user_id) {
        return learningMapper.learning_one_select(user_id);
    }
    
    public void learning_tag_insert(Integer learning_id,Integer tag_id) {
        learningMapper.learning_tag_insert(learning_id, tag_id);
    }
    
    public void tags_insert(String name) {
        learningMapper.tags_insert(name);
    }
    
    public Integer tags_search(String name) {
        return learningMapper.tags_search(name);
    }

    public int learning_update(Learning learning) {
        return learningMapper.learning_update(learning);
    }

    public void tags_delete(int learning_id) {
        learningMapper.tags_delete(learning_id);
    }

    public int learning_delete(int id, int userId) {
        return learningMapper.learning_delete(id, userId);
    }

    public void category_insert(String name) {
        learningMapper.category_insert(name);
    }

    // カテゴリーの名前変更
    public void category_update(int id, String name) {
        learningMapper.category_update(id, name);
    }

    // カテゴリーを使用している学習記録の件数
    public int category_usage_count(int id) {
        return learningMapper.category_usage_count(id);
    }

    // カテゴリーの削除
    public void category_delete(int id) {
        learningMapper.category_delete(id);
    }

    // タグの登録
    public void tag_insert(String name) {
        learningMapper.tags_insert(name);
    }

    // タグの名前変更
    public void tag_update(int id, String name) {
        learningMapper.tag_update(id, name);
    }

    // タグを使用している学習記録（紐づけ）の件数
    public int tag_usage_count(int id) {
        return learningMapper.tag_usage_count(id);
    }

    // タグの削除
    public void tag_delete(int id) {
        learningMapper.tag_delete(id);
    }

    // Proプラン「通知を希望する」の登録
    public void plan_interest_insert(PlanInterest planInterest) {
        learningMapper.plan_interest_insert(planInterest);
    }

    // 既に登録済みかどうか
    public boolean plan_interest_exists(int user_id) {
        return learningMapper.plan_interest_exists(user_id) > 0;
    }
}
