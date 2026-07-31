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
	List<categories> category_list();
	List<tags> tag_list();
	List<learning_tag> learning_tag();
	Integer learning_one_select(int user_id);
	int learning_insert(Learning learning);
	// 戻り値は更新件数。id+user_idの両方が一致した場合のみ1件更新される（本人以外の記録は0件のまま）
	int learning_update(Learning learning);
	void learning_tag_insert(Integer learning_id,Integer tag_id);
	void tags_insert(String name);
	Integer tags_search(String name);
	void tags_delete(int learning_id);
	// 戻り値は削除件数。id+user_idの両方が一致した場合のみ1件削除される（本人以外の記録は0件のまま）
	int learning_delete(@Param("id") int id, @Param("user_id") int user_id);
	void category_insert(String name);
	// カテゴリーの編集・削除・使用状況
	void category_update(@Param("id") int id, @Param("name") String name);
	void category_delete(int id);
	int category_usage_count(int id);
	// タグの編集・削除・使用状況
	void tag_update(@Param("id") int id, @Param("name") String name);
	void tag_delete(int id);
	int tag_usage_count(int id);
	// Proプラン「通知を希望する」
	void plan_interest_insert(PlanInterest planInterest);
	int plan_interest_exists(int user_id);
	// お問い合わせ
	void inquiry_insert(Inquiry inquiry);
	List<Inquiry> inquiry_list();
	int inquiry_status_update(@Param("id") int id, @Param("status") String status);
}
