package com.udemy.hello.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.udemy.hello.model.Note;
import com.udemy.hello.model.NoteTodoItem;

@Service
public class NoteService {

	@Autowired
	private NoteMapper noteMapper;

	// ユーザー別の全メモを取得し、todo項目の組み立てと実効進捗の算出まで行った状態で返す
	public List<Note> findAllForUser(int userId) {
		List<Note> notes = noteMapper.findAll(userId);
		List<NoteTodoItem> todoItems = noteMapper.findTodoItemsByUser(userId);
		Map<Integer, List<NoteTodoItem>> byNote = todoItems.stream()
				.collect(Collectors.groupingBy(NoteTodoItem::getNote_id));

		for (Note note : notes) {
			note.setTodo_items(byNote.getOrDefault(note.getId(), List.of()));
			note.setEffective_progress(ProgressCalculator.effectiveProgress(note));
		}
		return notes;
	}

	public int insert(Note note, List<NoteTodoItem> todoItems) {
		int result = noteMapper.insert(note);
		insertTodoItems(note.getId(), todoItems);
		return result;
	}

	// 戻り値は更新件数。本人が作成したメモ以外は0のまま（todoの入れ替えも行われない）
	public int update(Note note, List<NoteTodoItem> todoItems) {
		int updated = noteMapper.update(note);
		if (updated > 0) {
			// タグ更新と同じ流儀：既存todoを全削除してから入れ直す
			noteMapper.deleteTodoItemsByNote(note.getId());
			insertTodoItems(note.getId(), todoItems);
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

	public int delete(int id, int userId) {
		return noteMapper.delete(id, userId);
	}

	public int attach(int id, int userId, int actionPlanId) {
		return noteMapper.attach(id, userId, actionPlanId);
	}

	public int toggleTodo(int todoItemId, boolean checked, int userId) {
		return noteMapper.updateTodoItemChecked(todoItemId, checked, userId);
	}
}
