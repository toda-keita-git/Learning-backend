package com.udemy.hello.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.udemy.hello.model.Plan;

@Mapper
public interface PlanMapper {
	List<Plan> findAll(@Param("user_id") int user_id);
	int insert(Plan plan);
	// 兄弟内での新規作成時の表示順に使う（同じparent_idを持つ既存プランの末尾+1）
	int nextSortOrder(@Param("parent_id") Integer parent_id, @Param("user_id") int user_id);
	// 戻り値は更新件数。id+user_idの両方が一致した場合のみ1件更新される（本人以外は0件のまま）
	int update(Plan plan);
	// 進捗率からの自動status遷移専用（ProgressServiceが/plans取得のたびに呼ぶ）。
	// title/descriptionは含めず、statusだけを更新する
	int updateStatus(@Param("id") int id, @Param("status") String status, @Param("user_id") int user_id);
	// 親の変更（＝再配置）。循環チェックはPlanServiceがJava側で行ってから呼ぶ
	int updateParent(@Param("id") int id, @Param("parent_id") Integer parent_id, @Param("sort_order") int sort_order, @Param("user_id") int user_id);
	int updateSortOrder(@Param("id") int id, @Param("sort_order") int sort_order, @Param("user_id") int user_id);
	// 戻り値は削除件数。id+user_idの両方が一致した場合のみ1件削除される（本人以外は0件のまま）
	int delete(@Param("id") int id, @Param("user_id") int user_id);
}
