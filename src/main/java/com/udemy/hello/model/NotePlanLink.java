package com.udemy.hello.model;

import lombok.Data;

/**
 * note_plan_links の1行（メモID＋プランID）。
 * NoteServiceがユーザー分をまとめて取得し、メモごとにグルーピングして
 * Note.links に詰め直すための中間データ
 */
@Data
public class NotePlanLink {
	private Integer note_id;
	private Integer plan_id;
}
