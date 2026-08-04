package com.udemy.hello.model;

import java.sql.Timestamp;
import java.util.List;

import lombok.Data;

/**
 * メモ Entity。プランに従属しない独立した記録で、0件以上のプランに多対多でリンクできる。
 * 学習用(learning)・タスク用(task)・通常(normal) の3種別を1モデルで表現し、
 * typeに応じて mastery / progress / todo_items のうち使う項目が変わる。
 */
@Data
public class Note {
	private Integer id;
	private String type; // "learning" / "task" / "normal"
	private String title;
	private String body;
	private Integer mastery;  // type=learning のみ 0-100
	private Integer progress; // type=task かつ todo_items未使用の場合の手入力値 0-100
	private Integer category_id;
	// nullなら「繰り返しなし」。設定時は「今日の復習」タブでn日おきのやることとして表示される（頻度は自由設定）
	private Integer review_interval_days;
	private Timestamp created_at;
	private int delete_flg;
	private Integer user_id;
	// note_tags経由の多対多。DB列ではなく、NoteServiceが組み立てて詰める値
	private String[] tags;

	// DB列ではなく、NoteServiceが紐づくnote_todo_itemsから組み立てて詰める値
	private List<NoteTodoItem> todo_items;

	// DB列ではなく、NoteServiceがnote_plan_linksから組み立てて詰める値（リンク中のプランID一覧）
	private List<Integer> links;

	// DB列ではなく、NoteServiceがnote_attachmentsから組み立てて詰める値（画像・コード添付）
	private List<NoteAttachment> attachments;

	// DB列ではなく、NoteServiceが種別ごとのルールで算出して詰める値。
	// learning: mastery / task: todo_itemsがあれば達成率、無ければprogress / normal: 常にnull
	private Integer effective_progress;
}
