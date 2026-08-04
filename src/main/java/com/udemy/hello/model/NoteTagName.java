package com.udemy.hello.model;

import lombok.Data;

/**
 * note_tags と tags を結合した1行分（メモID＋タグ名）。
 * NoteServiceがユーザー分をまとめて取得し、メモごとにグルーピングして
 * Note.tagsに詰め直すための中間データ
 */
@Data
public class NoteTagName {
	private Integer note_id;
	private String name;
}
