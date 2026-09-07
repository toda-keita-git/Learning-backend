package com.udemy.hello.model;

import java.sql.Timestamp;
import java.time.LocalDate;

import lombok.Data;

/**
 * 「1日時間割」の1件分の予定。
 * source_type='custom'ならsource_idを持たない、時間割単独の予定。
 * それ以外（plan/note/habit）はsource_idでプラン・メモに紐づく（習慣もメモの一種として扱う）。
 * title_snapshotは作成時点のタイトルの複製で、紐づけ先が後から改名・削除されても
 * 時間割上の履歴としては元のタイトルのまま残す。
 */
@Data
public class DailyScheduleItem {
	private Long id;
	private Integer user_id;
	private LocalDate schedule_date;
	private Integer start_minute; // 0時からの経過分。0〜1439
	private Integer end_minute; // start_minuteより大きい。1〜1440
	private String timezone; // 表示・記録用。サーバー側の判定には使わない
	private String source_type; // "plan" / "note" / "habit" / "custom"
	private Integer source_id; // customの場合はnull
	private String title_snapshot;
	private String status; // "planned" / "done" / "skipped"
	private String color_key;
	private int delete_flg;
	private Timestamp created_at;
	private Timestamp updated_at;
}
