package com.udemy.hello.model;

import java.sql.Timestamp;

import lombok.Data;

/**
 * プラン Entity。目標・アクションプランを統合した再帰構造で、
 * parent_id が null なら「目標」、非nullなら「アクションプラン」としてUI側で表示される。
 * 親を変更するだけでどちらの役割にもなれる（仕様書「全体構造」章）。
 */
@Data
public class Plan {
	private Integer id;
	private Integer parent_id; // null = ルート（目標として表示）
	private String title;
	private String description;
	private String status; // not_started / in_progress / done / suspended
	private int sort_order; // 兄弟内の表示順。ドラッグ&ドロップで書き換わる
	private Timestamp created_at;
	private int delete_flg;
	private Integer user_id;

	// DB列ではなく、ProgressServiceが直属メモ・子プランから再帰的に算出して詰める値。
	// 対象が1件もない場合はnull（"未設定"。0%とは区別する）
	private Double progress;
}
