package com.udemy.hello;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.udemy.hello.Bean.GuestImportRequest;
import com.udemy.hello.mapper.GuestImportService;
import com.udemy.hello.security.JwtAuthInterceptor;

import jakarta.servlet.http.HttpServletRequest;

/**
 * ゲストモードのプラン・メモをログイン中のアカウントへ取り込む。
 * ログインしたばかりでもゲストデータが残っている場合にフロントが一度だけ呼ぶ想定
 * （販売可否評価レポート「4.5 ゲストデータがログイン後に引き継がれない」への対応）。
 */
@RestController
@CrossOrigin(origins = "${frontend.origin}")
public class GuestImportController {

	@Autowired
	private GuestImportService guestImportService;

	@PostMapping("/guest_import")
	public ResponseEntity<?> guestImport(@RequestBody GuestImportRequest body, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		try {
			GuestImportService.Result result = guestImportService.importGuestData(userId, body.getPlans(), body.getNotes());
			Map<String, Object> response = new HashMap<>();
			response.put("imported_plans", result.importedPlans);
			response.put("imported_notes", result.importedNotes);
			response.put("skipped_notes", result.skippedNotes);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}
}
