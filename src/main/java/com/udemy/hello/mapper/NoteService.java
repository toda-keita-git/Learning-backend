package com.udemy.hello.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.udemy.hello.model.Note;
import com.udemy.hello.model.NoteAttachment;
import com.udemy.hello.model.NotePlanLink;
import com.udemy.hello.model.NoteTagName;
import com.udemy.hello.model.NoteTodoItem;

@Service
public class NoteService {

	@Autowired
	private NoteMapper noteMapper;

	// タグはcategories/tagsと同じくユーザー単位の共有リソースのため、既存のタグ管理をそのまま使う
	@Autowired
	private LearningService learningService;

	// ユーザー別の全メモを取得し、todo項目・タグ・リンク・添付の組み立てと実効進捗の算出まで行った状態で返す
	public List<Note> findAllForUser(int userId) {
		List<Note> notes = noteMapper.findAll(userId);
		List<NoteTodoItem> todoItems = noteMapper.findTodoItemsByUser(userId);
		List<NoteTagName> tagNames = noteMapper.findTagNamesByUser(userId);
		List<NotePlanLink> links = noteMapper.findLinksByUser(userId);
		List<NoteAttachment> attachments = noteMapper.findAttachmentsByUser(userId);

		Map<Integer, List<NoteTodoItem>> todosByNote = todoItems.stream()
				.collect(Collectors.groupingBy(NoteTodoItem::getNote_id));
		Map<Integer, List<String>> tagsByNote = tagNames.stream()
				.collect(Collectors.groupingBy(NoteTagName::getNote_id,
						Collectors.mapping(NoteTagName::getName, Collectors.toList())));
		Map<Integer, List<Integer>> linksByNote = links.stream()
				.collect(Collectors.groupingBy(NotePlanLink::getNote_id,
						Collectors.mapping(NotePlanLink::getPlan_id, Collectors.toList())));
		Map<Integer, List<NoteAttachment>> attachmentsByNote = attachments.stream()
				.collect(Collectors.groupingBy(NoteAttachment::getNote_id));

		for (Note note : notes) {
			note.setTodo_items(todosByNote.getOrDefault(note.getId(), List.of()));
			note.setTags(tagsByNote.getOrDefault(note.getId(), List.of()).toArray(new String[0]));
			note.setLinks(linksByNote.getOrDefault(note.getId(), List.of()));
			note.setAttachments(attachmentsByNote.getOrDefault(note.getId(), List.of()));
			note.setEffective_progress(ProgressCalculator.effectiveProgress(note));
		}
		return notes;
	}

	// 無料プランの登録上限チェック用（本人の削除されていないメモ数）
	public int count(int userId) {
		return noteMapper.count(userId);
	}

	// 作成時に渡された初期リンク・初期添付があれば、そのまま張っておく（都度2回に分けて呼ばなくて済むように）
	public int insert(Note note, List<NoteTodoItem> todoItems, String[] tagNames) {
		int result = noteMapper.insert(note);
		insertTodoItems(note.getId(), todoItems);
		insertTags(note.getId(), tagNames, note.getUser_id());
		if (note.getLinks() != null) {
			for (Integer planId : note.getLinks()) {
				noteMapper.insertLink(note.getId(), planId, note.getUser_id());
			}
		}
		if (note.getAttachments() != null) {
			for (NoteAttachment attachment : note.getAttachments()) {
				attachment.setNote_id(note.getId());
				noteMapper.insertAttachment(attachment, note.getUser_id());
			}
		}
		return result;
	}

	// 戻り値は更新件数。本人が作成したメモ以外は0のまま（todo・タグの入れ替えも行われない）。
	// リンク・添付は専用のエンドポイント（attach/detach/attachment系）で管理するため、ここでは触らない
	public int update(Note note, List<NoteTodoItem> todoItems, String[] tagNames) {
		int updated = noteMapper.update(note);
		if (updated > 0) {
			// タグ更新と同じ流儀：既存todo・タグを全削除してから入れ直す
			noteMapper.deleteTodoItemsByNote(note.getId());
			insertTodoItems(note.getId(), todoItems);
			noteMapper.deleteNoteTagsByNote(note.getId());
			insertTags(note.getId(), tagNames, note.getUser_id());
		}
		return updated;
	}

	private void insertTodoItems(int noteId, List<NoteTodoItem> todoItems) {
		if (todoItems == null) {
			return;
		}
		int order = 0;
		for (NoteTodoItem item : todoItems) {
			item.setNote_id(noteId);
			item.setSort_order(order++);
			noteMapper.insertTodoItem(item);
		}
	}

	// タグの存在確認と挿入は、フリープランのタグ上限に達している場合は新規タグ作成をスキップする
	// （メモ自体の保存は続行し、上限超過分のタグだけが付かない形になる。learning_insertと同じ方針）
	private void insertTags(int noteId, String[] tagNames, int userId) {
		if (tagNames == null) {
			return;
		}
		for (String name : tagNames) {
			boolean exists = learningService.tag_list(userId).stream().anyMatch(t -> t.getName().equals(name));
			if (!exists && learningService.tag_count(userId) < LearningService.FREE_TAG_LIMIT) {
				learningService.tags_insert(name, userId);
			}
		}
		for (String name : tagNames) {
			Integer tagId = learningService.tags_search(name, userId);
			if (tagId != null) {
				noteMapper.insertNoteTag(noteId, tagId);
			}
		}
	}

	public int delete(int id, int userId) {
		return noteMapper.delete(id, userId);
	}

	// アカウントデータの全削除用（アカウント自体は残す）
	public void deleteAllForUser(int userId) {
		noteMapper.deleteAllForUser(userId);
	}

	public int toggleTodo(int todoItemId, boolean checked, int userId) {
		return noteMapper.updateTodoItemChecked(todoItemId, checked, userId);
	}

	// メモをプランへリンク（ドラッグ／タップどちらの操作からも同じAPIを呼ぶ）
	public int link(int noteId, int planId, int userId) {
		return noteMapper.insertLink(noteId, planId, userId);
	}

	public int unlink(int noteId, int planId, int userId) {
		return noteMapper.deleteLink(noteId, planId, userId);
	}

	public int addAttachment(NoteAttachment attachment, int userId) {
		return noteMapper.insertAttachment(attachment, userId);
	}

	public int deleteAttachment(int attachmentId, int userId) {
		return noteMapper.deleteAttachment(attachmentId, userId);
	}
}
