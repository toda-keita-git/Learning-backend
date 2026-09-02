package com.udemy.hello.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.udemy.hello.model.Note;
import com.udemy.hello.model.NoteAttachment;
import com.udemy.hello.model.NotePlanLink;
import com.udemy.hello.model.NoteTagName;
import com.udemy.hello.model.NoteTodoItem;

@Mapper
public interface NoteMapper {
	List<Note> findAll(@Param("user_id") int user_id);
	int insert(Note note);
	// 無料プランの登録上限チェック用（本人の削除されていないメモ数）
	int count(@Param("user_id") int user_id);
	// 戻り値は更新件数。id+user_idの両方が一致した場合のみ1件更新される（本人以外は0件のまま）
	int update(Note note);
	// 戻り値は削除件数。id+user_idの両方が一致した場合のみ1件削除される（本人以外は0件のまま）
	int delete(@Param("id") int id, @Param("user_id") int user_id);
	// アカウントデータの全削除（アカウント自体は残す）で使う。本人の未削除メモを一括で論理削除する
	int deleteAllForUser(@Param("user_id") int user_id);

	// 本人の全メモに紐づくtodo項目をまとめて取得し、Note組み立て時にnote_idでグルーピングする
	List<NoteTodoItem> findTodoItemsByUser(@Param("user_id") int user_id);
	void insertTodoItem(NoteTodoItem item);
	void deleteTodoItemsByNote(@Param("note_id") int note_id);
	// todo1件のチェック切替。note_idの所有者がuser_idであるものだけを対象にする。戻り値は更新件数
	int updateTodoItemChecked(@Param("id") int id, @Param("checked") boolean checked, @Param("user_id") int user_id);

	// 本人の全メモに紐づくタグ名。NoteTagNameのnote_idでグルーピングしてNote.tagsに詰め直す
	List<NoteTagName> findTagNamesByUser(@Param("user_id") int user_id);
	void insertNoteTag(@Param("note_id") int note_id, @Param("tag_id") int tag_id);
	void deleteNoteTagsByNote(@Param("note_id") int note_id);

	// メモ⇄プランの多対多リンク。本人の削除されていないプランへのリンクだけを返す
	List<NotePlanLink> findLinksByUser(@Param("user_id") int user_id);
	// 戻り値は挿入件数。note_id+user_idが一致するメモが無ければ0件のまま（他人のメモへは付けられない）
	int insertLink(@Param("note_id") int note_id, @Param("plan_id") int plan_id, @Param("user_id") int user_id);
	int deleteLink(@Param("note_id") int note_id, @Param("plan_id") int plan_id, @Param("user_id") int user_id);

	// メモへの複数添付（画像・コード）。本人のメモに紐づくものだけを返す
	List<NoteAttachment> findAttachmentsByUser(@Param("user_id") int user_id);
	// 戻り値は挿入件数。note_id+user_idが一致するメモが無ければ0件のまま
	int insertAttachment(@Param("attachment") NoteAttachment attachment, @Param("user_id") int user_id);
	int deleteAttachment(@Param("id") int id, @Param("user_id") int user_id);
}
