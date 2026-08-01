package com.udemy.hello.mapper;

import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.udemy.hello.model.Learning;
import com.udemy.hello.model.categories;
import com.udemy.hello.model.tags;
import com.udemy.hello.model.learning_tag;
import com.udemy.hello.model.PlanInterest;
import com.udemy.hello.model.Inquiry;

@Mapper
public interface LearningMapper {
	List<Learning> findAll(int user_id);
	// カテゴリー・タグは個人ごとに管理するため、一覧取得は本人のものだけに絞る
	List<categories> category_list(@Param("user_id") int user_id);
	List<tags> tag_list(@Param("user_id") int user_id);
	List<learning_tag> learning_tag(@Param("user_id") int user_id);
	Integer learning_one_select(int user_id);
	// 無料プランの登録上限チェック用（本人の削除されていない記録数）
	int learning_count(@Param("user_id") int user_id);
	int learning_insert(Learning learning);
	// 戻り値は更新件数。id+user_idの両方が一致した場合のみ1件更新される（本人以外の記録は0件のまま）
	int learning_update(Learning learning);
	void learning_tag_insert(Integer learning_id,Integer tag_id);
	void tags_insert(@Param("name") String name, @Param("user_id") int user_id);
	Integer tags_search(@Param("name") String name, @Param("user_id") int user_id);
	void tags_delete(int learning_id);
	// 戻り値は削除件数。id+user_idの両方が一致した場合のみ1件削除される（本人以外の記録は0件のまま）
	int learning_delete(@Param("id") int id, @Param("user_id") int user_id);
	void category_insert(@Param("name") String name, @Param("user_id") int user_id);
	// カテゴリーの編集・削除・使用状況（戻り値は更新・削除件数。本人が作成したもの以外は0件のまま）
	int category_update(@Param("id") int id, @Param("name") String name, @Param("user_id") int user_id);
	int category_delete(@Param("id") int id, @Param("user_id") int user_id);
	int category_usage_count(@Param("id") int id, @Param("user_id") int user_id);
	// タグの編集・削除・使用状況
	int tag_update(@Param("id") int id, @Param("name") String name, @Param("user_id") int user_id);
	int tag_delete(@Param("id") int id, @Param("user_id") int user_id);
	int tag_usage_count(@Param("id") int id, @Param("user_id") int user_id);
	// Proプラン「通知を希望する」
	void plan_interest_insert(PlanInterest planInterest);
	int plan_interest_exists(int user_id);
	// お問い合わせ
	void inquiry_insert(Inquiry inquiry);
	List<Inquiry> inquiry_list();
	int inquiry_status_update(@Param("id") int id, @Param("status") String status);
}
