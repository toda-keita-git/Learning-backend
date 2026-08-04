package com.udemy.hello;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.udemy.hello.Bean.ActionPlanPriorityItem;
import com.udemy.hello.mapper.ActionPlanService;
import com.udemy.hello.model.ActionPlan;
import com.udemy.hello.security.JwtAuthInterceptor;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(origins = "${frontend.origin}")
public class ActionPlanController {

	@Autowired
	ActionPlanService actionPlanService;

	// 本人の全アクションプラン一覧（goal_id込み）を、紐づくメモから算出した達成率込みで返す。
	// 目標別の絞り込みはフロント側で行う（/goalsと同じ流儀）
	@GetMapping("/action_plans")
	public List<ActionPlan> actionPlans(HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		return actionPlanService.findAllWithProgress(userId);
	}

	@PostMapping("/action_plan_insert")
	public ResponseEntity<String> action_plan_insert(@RequestBody ActionPlan actionPlan, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		actionPlan.setUser_id(userId);
		if (actionPlan.getStatus() == null || actionPlan.getStatus().isBlank()) {
			actionPlan.setStatus("not_started");
		}
		actionPlan.setCreated_at(new Timestamp(System.currentTimeMillis()));
		actionPlanService.insert(actionPlan);
		return ResponseEntity.ok("inserted");
	}

	@PostMapping("/action_plan_update/{id}")
	public ResponseEntity<String> action_plan_update(@PathVariable("id") int id, @RequestBody ActionPlan actionPlan, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		actionPlan.setId(id);
		actionPlan.setUser_id(userId);
		int updated = actionPlanService.update(actionPlan);
		if (updated == 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("このアクションプランを編集する権限がありません。");
		}
		return ResponseEntity.ok("updated");
	}

	// ドラッグ&ドロップでの並べ替え確定時に一括送信される
	@PostMapping("/action_plan_reorder")
	public ResponseEntity<String> action_plan_reorder(@RequestBody List<ActionPlanPriorityItem> items, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		actionPlanService.reorder(items, userId);
		return ResponseEntity.ok("reordered");
	}

	@PostMapping("/action_plan_delete/{id}")
	public ResponseEntity<String> action_plan_delete(@PathVariable("id") int id, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		int deleted = actionPlanService.delete(id, userId);
		if (deleted == 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("このアクションプランを削除する権限がありません。");
		}
		return ResponseEntity.ok("deleted");
	}
}
