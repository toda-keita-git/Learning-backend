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

import com.udemy.hello.mapper.GoalService;
import com.udemy.hello.model.Goal;
import com.udemy.hello.security.JwtAuthInterceptor;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(origins = "${frontend.origin}")
public class GoalController {

	@Autowired
	GoalService goalService;

	// 本人の目標一覧を、配下のアクションプランから算出した達成率込みで返す
	@GetMapping("/goals")
	public List<Goal> goals(HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		return goalService.findAllWithProgress(userId);
	}

	@PostMapping("/goal_insert")
	public ResponseEntity<String> goal_insert(@RequestBody Goal goal, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		goal.setUser_id(userId);
		if (goal.getStatus() == null || goal.getStatus().isBlank()) {
			goal.setStatus("in_progress");
		}
		goal.setCreated_at(new Timestamp(System.currentTimeMillis()));
		goalService.insert(goal);
		return ResponseEntity.ok("inserted");
	}

	@PostMapping("/goal_update/{id}")
	public ResponseEntity<String> goal_update(@PathVariable("id") int id, @RequestBody Goal goal, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		goal.setId(id);
		goal.setUser_id(userId);
		int updated = goalService.update(goal);
		if (updated == 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("この目標を編集する権限がありません。");
		}
		return ResponseEntity.ok("updated");
	}

	@PostMapping("/goal_delete/{id}")
	public ResponseEntity<String> goal_delete(@PathVariable("id") int id, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		int deleted = goalService.delete(id, userId);
		if (deleted == 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("この目標を削除する権限がありません。");
		}
		return ResponseEntity.ok("deleted");
	}
}
