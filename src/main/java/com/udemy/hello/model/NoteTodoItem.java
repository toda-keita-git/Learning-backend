package com.udemy.hello.model;

import lombok.Data;

/**
 * チェックリスト用メモに付けるチェック付きtodo項目
 */
@Data
public class NoteTodoItem {
	private Integer id;
	private Integer note_id;
	private String label;
	private boolean checked;
	private int sort_order;
}
