package com.udemy.hello.mapper;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.udemy.hello.Bean.ActionPlanPriorityItem;
import com.udemy.hello.model.ActionPlan;

@Service
public class ActionPlanService {

	@Autowired
	private ActionPlanMapper actionPlanMapper;

	@Autowired
	private NoteMapper noteMapper;

	@Autowired
	private ProgressService progressService;

	public List<ActionPlan> findAllWithProgress(int userId) {
		return progressService.listActionPlansWithProgress(userId);
	}

	// 新規作成時の表示順は、同じ目標内の既存アクションプランの末尾に自動採番する
	public int insert(ActionPlan actionPlan) {
		int next = actionPlanMapper.nextPriority(actionPlan.getGoal_id(), actionPlan.getUser_id());
		actionPlan.setPriority(next);
		return actionPlanMapper.insert(actionPlan);
	}

	public int update(ActionPlan actionPlan) {
		return actionPlanMapper.update(actionPlan);
	}

	// ドラッグ&ドロップ確定後、送られてきた並び順を1件ずつ書き換える
	public void reorder(List<ActionPlanPriorityItem> items, int userId) {
		for (ActionPlanPriorityItem item : items) {
			actionPlanMapper.updatePriority(item.getId(), item.getPriority(), userId);
		}
	}

	// 削除時、紐づくメモは削除せず未紐付けに戻す（仕様書「移行計画」章の方針と同様、記録は残す）
	public int delete(int id, int userId) {
		int deleted = actionPlanMapper.delete(id, userId);
		if (deleted > 0) {
			noteMapper.detachByActionPlan(id);
		}
		return deleted;
	}
}
