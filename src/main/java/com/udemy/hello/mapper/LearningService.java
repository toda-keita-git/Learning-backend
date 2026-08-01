package com.udemy.hello.mapper;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.udemy.hello.model.Learning;
import com.udemy.hello.model.categories;
import com.udemy.hello.model.tags;
import com.udemy.hello.model.learning_tag;
import com.udemy.hello.model.PlanInterest;
import com.udemy.hello.model.Inquiry;

@Service
public class LearningService {

    // 新規ユーザー登録時に自動で用意するカテゴリー・タグの初期セット（一般＋プログラミング関連）
    private static final List<String> DEFAULT_CATEGORIES = List.of(
        "仕事", "学業", "資格・検定", "語学", "趣味", "健康・生活",
        "プログラミング", "Web開発", "インフラ・クラウド", "データベース"
    );

    private static final List<String> DEFAULT_TAGS = List.of(
        "メモ", "復習", "重要", "あとで",
        "JavaScript", "TypeScript", "Python", "Java", "SQL",
        "Git", "React", "Spring", "Docker", "AWS"
    );

    @Autowired
    private LearningMapper learningMapper;

    // ユーザーID指定で全学習情報取得
    public List<Learning> findALL(int user_id) {
        return learningMapper.findAll(user_id);
    }

    // カテゴリー・タグは個人ごとに管理するため、本人のものだけを取得する
    public List<categories> category_list(int userId) {
        return learningMapper.category_list(userId);
    }

    public List<tags> tag_list(int userId) {
        return learningMapper.tag_list(userId);
    }

    public List<learning_tag> learning_tag(int userId) {
        return learningMapper.learning_tag(userId);
    }

    // 学習情報登録（user_idを含む）
    public int learning_insert(Learning learning) {
        return learningMapper.learning_insert(learning);
    }

    // 無料プランの登録上限チェック用（本人の削除されていない記録数）
    public int learning_count(int userId) {
        return learningMapper.learning_count(userId);
    }

    // 最新学習IDを取得（user_id指定）
    public Integer learning_one_select(int user_id) {
        return learningMapper.learning_one_select(user_id);
    }

    public void learning_tag_insert(Integer learning_id,Integer tag_id) {
        learningMapper.learning_tag_insert(learning_id, tag_id);
    }

    public void tags_insert(String name, int userId) {
        learningMapper.tags_insert(name, userId);
    }

    public Integer tags_search(String name, int userId) {
        return learningMapper.tags_search(name, userId);
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

    public void category_insert(String name, int userId) {
        learningMapper.category_insert(name, userId);
    }

    // カテゴリーの名前変更（戻り値は更新件数。本人が作成したもの以外は0件のまま）
    public int category_update(int id, String name, int userId) {
        return learningMapper.category_update(id, name, userId);
    }

    // カテゴリーを使用している（本人の）学習記録の件数
    public int category_usage_count(int id, int userId) {
        return learningMapper.category_usage_count(id, userId);
    }

    // カテゴリーの削除（戻り値は削除件数。本人が作成したもの以外は0件のまま）
    public int category_delete(int id, int userId) {
        return learningMapper.category_delete(id, userId);
    }

    // タグの登録
    public void tag_insert(String name, int userId) {
        learningMapper.tags_insert(name, userId);
    }

    // タグの名前変更（戻り値は更新件数。本人が作成したもの以外は0件のまま）
    public int tag_update(int id, String name, int userId) {
        return learningMapper.tag_update(id, name, userId);
    }

    // タグを使用している（本人の）学習記録（紐づけ）の件数
    public int tag_usage_count(int id, int userId) {
        return learningMapper.tag_usage_count(id, userId);
    }

    // タグの削除（戻り値は削除件数。本人が作成したもの以外は0件のまま）
    public int tag_delete(int id, int userId) {
        return learningMapper.tag_delete(id, userId);
    }

    // 新規ユーザー登録時に、一般＋プログラミング関連の初期カテゴリー・タグを用意する
    public void seedDefaultCategoriesAndTags(int userId) {
        for (String name : DEFAULT_CATEGORIES) {
            learningMapper.category_insert(name, userId);
        }
        for (String name : DEFAULT_TAGS) {
            learningMapper.tags_insert(name, userId);
        }
    }

    // Proプラン「通知を希望する」の登録
    public void plan_interest_insert(PlanInterest planInterest) {
        learningMapper.plan_interest_insert(planInterest);
    }

    // 既に登録済みかどうか
    public boolean plan_interest_exists(int user_id) {
        return learningMapper.plan_interest_exists(user_id) > 0;
    }

    // お問い合わせの登録
    public void inquiry_insert(Inquiry inquiry) {
        learningMapper.inquiry_insert(inquiry);
    }

    // お問い合わせ一覧（新しい順）
    public List<Inquiry> inquiry_list() {
        return learningMapper.inquiry_list();
    }

    // お問い合わせのステータス更新
    public int inquiry_status_update(int id, String status) {
        return learningMapper.inquiry_status_update(id, status);
    }
}
