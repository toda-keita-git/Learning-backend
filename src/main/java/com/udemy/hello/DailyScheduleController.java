package com.udemy.hello;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.udemy.hello.mapper.DailyScheduleService;
import com.udemy.hello.model.DailyScheduleItem;
import com.udemy.hello.security.JwtAuthInterceptor;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(origins = "${frontend.origin}")
public class DailyScheduleController {

	@Autowired
	private DailyScheduleService dailyScheduleService;

	// 指定日の、本人の予定だけを返す
	@GetMapping("/daily_schedule")
	public List<DailyScheduleItem> daily_schedule(@RequestParam("date") String date, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		return dailyScheduleService.findByUserAndDate(userId, parseDate(date));
	}

	@PostMapping("/daily_schedule_insert")
	public ResponseEntity<Map<String, Long>> daily_schedule_insert(@RequestBody DailyScheduleItem item,
			HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		item.setUser_id(userId);
		dailyScheduleService.insert(item);
		return ResponseEntity.ok(Map.of("id", item.getId()));
	}

	@PostMapping("/daily_schedule_update/{id}")
	public ResponseEntity<String> daily_schedule_update(@PathVariable("id") long id,
			@RequestBody DailyScheduleItem item, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		item.setId(id);
		boolean updated = dailyScheduleService.update(item, userId);
		if (!updated) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("この予定を編集する権限がありません。");
		}
		return ResponseEntity.ok("updated");
	}

	// 完了／未完了・スキップの切り替え専用。body: {status}
	@PostMapping("/daily_schedule_complete/{id}")
	public ResponseEntity<String> daily_schedule_complete(@PathVariable("id") long id,
			@RequestBody Map<String, String> body, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		String status = body.get("status");
		if (status == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("statusを指定してください。");
		}
		boolean updated = dailyScheduleService.updateStatus(id, status, userId);
		if (!updated) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("この予定を編集する権限がありません。");
		}
		return ResponseEntity.ok("updated");
	}

	@PostMapping("/daily_schedule_delete/{id}")
	public ResponseEntity<String> daily_schedule_delete(@PathVariable("id") long id, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		boolean deleted = dailyScheduleService.delete(id, userId);
		if (!deleted) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("この予定を削除する権限がありません。");
		}
		return ResponseEntity.ok("deleted");
	}

	private LocalDate parseDate(String date) {
		try {
			return LocalDate.parse(date);
		} catch (DateTimeParseException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dateの形式が不正です（YYYY-MM-DD）。");
		}
	}
}
