package com.udemy.hello.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.udemy.hello.model.ActionPlan;

@Mapper
public interface ActionPlanMapper {
	List<ActionPlan> findAll(@Param("user_id") int user_id);
	int insert(ActionPlan actionPlan);
	// 新規作成時の表示順は、同じ目標内の既存アクションプランの末尾（最大priority+1）に自動採番する
	int nextPriority(@Param("goal_id") int goal_id, @Param("user_id") int user_id);
	// 戻り値は更新件数。id+user_idの両方が一致した場合のみ1件更新される（本人以外は0件のまま）
	int update(ActionPlan actionPlan);
	int updatePriority(@Param("id") int id, @Param("priority") int priority, @Param("user_id") int user_id);
	// 戻り値は削除件数。id+user_idの両方が一致した場合のみ1件削除される（本人以外は0件のまま）
	int delete(@Param("id") int id, @Param("user_id") int user_id);
}
