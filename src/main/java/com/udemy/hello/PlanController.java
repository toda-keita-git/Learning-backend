package com.udemy.hello;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.udemy.hello.Bean.PlanReorderItem;
import com.udemy.hello.mapper.PlanService;
import com.udemy.hello.mapper.ProgressService;
import com.udemy.hello.model.Plan;
import com.udemy.hello.security.JwtAuthInterceptor;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(origins = "${frontend.origin}")
public class PlanController {

	@Autowired
	PlanService planService;

	@Autowired
	ProgressService progressService;

	// 本人の全プラン一覧（parent_id込み）を、達成率を再帰的に算出した状態で返す。
	// ルート/子の絞り込み・並び替えはフロント側で行う（/notesと同じ流儀）
	@GetMapping("/plans")
	public List<Plan> plans(HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		return progressService.listPlansWithProgress(userId);
	}

	// 戻り値のidは、ドラッグでメモを「新しいプランとして保存」した直後にそのままlink APIへ渡すために使う
	@PostMapping("/plan_insert")
	public ResponseEntity<Map<String, Integer>> plan_insert(@RequestBody Plan plan, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		plan.setUser_id(userId);
		if (plan.getStatus() == null || plan.getStatus().isBlank()) {
			plan.setStatus("not_started");
		}
		plan.setCreated_at(new Timestamp(System.currentTimeMillis()));
		planService.insert(plan);
		return ResponseEntity.ok(Map.of("id", plan.getId()));
	}

	@PostMapping("/plan_update/{id}")
	public ResponseEntity<String> plan_update(@PathVariable("id") int id, @RequestBody Plan plan, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		plan.setId(id);
		plan.setUser_id(userId);
		int updated = planService.update(plan);
		if (updated == 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("このプランを編集する権限がありません。");
		}
		return ResponseEntity.ok("updated");
	}

	// 親の変更（＝再配置）。ドラッグでの移動・タップでの移動どちらもここを呼ぶ。body: {parent_id}（ルート化する場合はnull）
	@PostMapping("/plan_reparent/{id}")
	public ResponseEntity<String> plan_reparent(@PathVariable("id") int id, @RequestBody Map<String, Integer> body, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		Integer parentId = body.get("parent_id");
		boolean ok = planService.reparent(id, parentId, userId);
		if (!ok) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("移動先が不正です（循環になる、または権限がありません）。");
		}
		return ResponseEntity.ok("reparented");
	}

	// 同じ親を持つプラン同士の並べ替え確定時に一括送信される
	@PostMapping("/plan_reorder")
	public ResponseEntity<String> plan_reorder(@RequestBody List<PlanReorderItem> items, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		planService.reorder(items, userId);
		return ResponseEntity.ok("reordered");
	}

	@PostMapping("/plan_delete/{id}")
	public ResponseEntity<String> plan_delete(@PathVariable("id") int id, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		int deleted = planService.delete(id, userId);
		if (deleted == 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("このプランを削除する権限がありません。");
		}
		return ResponseEntity.ok("deleted");
	}
}
