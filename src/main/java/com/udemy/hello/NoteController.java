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
import com.udemy.hello.model.NoteAttachment;
import com.udemy.hello.security.JwtAuthInterceptor;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(origins = "${frontend.origin}")
public class NoteController {

	private static final Set<String> VALID_TYPES = Set.of("learning", "task", "normal");
	private static final Set<String> VALID_ATTACHMENT_KINDS = Set.of("image", "code");

	// メモの登録上限（旧learningsと同じ100件。既存のメモは削除されず、新規登録だけがブロックされる）。
	// 変更する場合は、利用者に案内しているPricingPlanDialog.tsxのLIMITSも必ず合わせること
	private static final int FREE_PLAN_LIMIT = 100;

	@Autowired
	NoteService noteService;

	// 本人の全メモをフラットに返す。プラン単位のタイムライン・メモライブラリの絞り込みはフロント側で行う
	@GetMapping("/notes")
	public List<Note> notes(HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		return noteService.findAllForUser(userId);
	}

	// メモは特定のプランに従属しない。作成時にlinksを含めれば、その場でプランへのリンクも張られる
	@PostMapping("/note_insert")
	public ResponseEntity<String> note_insert(@RequestBody Note note, HttpServletRequest request) {
		if (!VALID_TYPES.contains(note.getType())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("メモの種別が不正です。");
		}
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		if (noteService.count(userId) >= FREE_PLAN_LIMIT) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body("メモの登録上限（" + FREE_PLAN_LIMIT + "件）に達しています。不要なメモを削除すると、新しく作成できます。");
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

	// メモをプランへリンク。ドラッグでの操作・カード上のタップ操作、どちらからもこのAPIを呼ぶ
	@PostMapping("/note_link/{id}")
	public ResponseEntity<String> note_link(@PathVariable("id") int id, @RequestBody Map<String, Integer> body, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		Integer planId = body.get("plan_id");
		if (planId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("リンク先のプランを指定してください。");
		}
		int inserted = noteService.link(id, planId, userId);
		if (inserted == 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("このメモをリンクする権限がありません。");
		}
		return ResponseEntity.ok("linked");
	}

	@PostMapping("/note_unlink/{id}")
	public ResponseEntity<String> note_unlink(@PathVariable("id") int id, @RequestBody Map<String, Integer> body, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		Integer planId = body.get("plan_id");
		if (planId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("外すリンク先のプランを指定してください。");
		}
		int deleted = noteService.unlink(id, planId, userId);
		if (deleted == 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("このメモのリンクを外す権限がありません。");
		}
		return ResponseEntity.ok("unlinked");
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

	// 画像／コードの添付を1件追加（実体はGitHubリポジトリ側に保存済みで、ここではパスだけを受け取る）
	@PostMapping("/note_attachment_insert/{id}")
	public ResponseEntity<String> note_attachment_insert(@PathVariable("id") int id, @RequestBody NoteAttachment attachment, HttpServletRequest request) {
		if (!VALID_ATTACHMENT_KINDS.contains(attachment.getKind())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("添付の種別が不正です。");
		}
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		attachment.setNote_id(id);
		int inserted = noteService.addAttachment(attachment, userId);
		if (inserted == 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("このメモに添付を追加する権限がありません。");
		}
		return ResponseEntity.ok("inserted");
	}

	@PostMapping("/note_attachment_delete/{attachmentId}")
	public ResponseEntity<String> note_attachment_delete(@PathVariable("attachmentId") int attachmentId, HttpServletRequest request) {
		int userId = JwtAuthInterceptor.getVerifiedUserId(request);
		int deleted = noteService.deleteAttachment(attachmentId, userId);
		if (deleted == 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("この添付を削除する権限がありません。");
		}
		return ResponseEntity.ok("deleted");
	}
}
