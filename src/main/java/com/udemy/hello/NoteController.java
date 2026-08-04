package com.udemy.hello;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.udemy.hello.mapper.NoteService;
import com.udemy.hello.model.Note;
import com.udemy.hello.security.JwtAuthInterceptor;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(origins = "${frontend.origin}")
public class NoteController {

	private static final Set<String> VALID_TYPES = Set.of("learning", "task", "normal");

	// フリープランの登録上限（旧learningsと同じ100件。既存のメモは削除されず、新規登録だけがブロックされる）
	private static final int FREE_PLAN_LIMIT = 100;

	@Autowired
	NoteService noteService;

	// 本人の全メモをフラットに返す。振り返りタイムライン・未紐付け一覧はフロント側でフィルタする
	@GetMapping("/notes")
	public List<Note> notes(HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		return noteService.findAllForUser(userId);
	}

	@PostMapping("/note_insert")
	public ResponseEntity<String> note_insert(@RequestBody Note note, HttpServletRequest request) {
		if (!VALID_TYPES.contains(note.getType())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("メモの種別が不正です。");
		}
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		if (noteService.count(userId) >= FREE_PLAN_LIMIT) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body("フリープランの登録上限（" + FREE_PLAN_LIMIT + "件）に達しています。Proプランのご案内をご確認ください。");
		}
		note.setUser_id(userId);
		note.setCreated_at(new Timestamp(System.currentTimeMillis()));
		noteService.insert(note, note.getTodo_items(), note.getTags());
		return ResponseEntity.ok("inserted");
	}

	@PostMapping("/note_update/{id}")
	public ResponseEntity<String> note_update(@PathVariable("id") int id, @RequestBody Note note, HttpServletRequest request) {
		if (!VALID_TYPES.contains(note.getType())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("メモの種別が不正です。");
		}
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		note.setId(id);
		note.setUser_id(userId);
		int updated = noteService.update(note, note.getTodo_items(), note.getTags());
		if (updated == 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("このメモを編集する権限がありません。");
		}
		return ResponseEntity.ok("updated");
	}

	@PostMapping("/note_delete/{id}")
	public ResponseEntity<String> note_delete(@PathVariable("id") int id, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		int deleted = noteService.delete(id, userId);
		if (deleted == 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("このメモを削除する権限がありません。");
		}
		return ResponseEntity.ok("deleted");
	}

	// 未紐付けメモを後からアクションプランに紐付ける
	@PostMapping("/note_attach/{id}")
	public ResponseEntity<String> note_attach(@PathVariable("id") int id, @RequestBody Map<String, Integer> body, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		Integer actionPlanId = body.get("action_plan_id");
		if (actionPlanId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("紐付け先のアクションプランを指定してください。");
		}
		int updated = noteService.attach(id, userId, actionPlanId);
		if (updated == 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("このメモを紐付ける権限がありません。");
		}
		return ResponseEntity.ok("attached");
	}

	// todo1件のチェック切替。振り返りタイムライン閲覧中の軽い操作用に、フルのnote_updateとは別に用意する
	@PostMapping("/note_todo_toggle/{id}")
	public ResponseEntity<String> note_todo_toggle(@PathVariable("id") int id, @RequestBody Map<String, Boolean> body, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		Boolean checked = body.get("checked");
		if (checked == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("チェック状態を指定してください。");
		}
		int updated = noteService.toggleTodo(id, checked, userId);
		if (updated == 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("このtodoを操作する権限がありません。");
		}
		return ResponseEntity.ok("toggled");
	}
}
