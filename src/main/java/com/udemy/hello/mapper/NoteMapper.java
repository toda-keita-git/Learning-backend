package com.udemy.hello.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.udemy.hello.model.Note;
import com.udemy.hello.model.NoteTodoItem;

@Mapper
public interface NoteMapper {
	List<Note> findAll(@Param("user_id") int user_id);
	int insert(Note note);
	// 戻り値は更新件数。id+user_idの両方が一致した場合のみ1件更新される（本人以外は0件のまま）
	int update(Note note);
	// 戻り値は削除件数。id+user_idの両方が一致した場合のみ1件削除される（本人以外は0件のまま）
	int delete(@Param("id") int id, @Param("user_id") int user_id);
	// 未紐付けメモを後からアクションプランへ紐付ける。戻り値は更新件数
	int attach(@Param("id") int id, @Param("user_id") int user_id, @Param("action_plan_id") int action_plan_id);
	// アクションプラン削除時、紐づくメモを未紐付けに戻す
	void detachByActionPlan(@Param("action_plan_id") int action_plan_id);

	// 本人の全メモに紐づくtodo項目をまとめて取得し、Note組み立て時にnote_idでグルーピングする
	List<NoteTodoItem> findTodoItemsByUser(@Param("user_id") int user_id);
	void insertTodoItem(NoteTodoItem item);
	void deleteTodoItemsByNote(@Param("note_id") int note_id);
	// todo1件のチェック切替。note_idの所有者がuser_idであるものだけを対象にする。戻り値は更新件数
	int updateTodoItemChecked(@Param("id") int id, @Param("checked") boolean checked, @Param("user_id") int user_id);
}
