package com.udemy.hello.model;

import java.sql.Timestamp;

import lombok.Data;

/**
 * アクションプラン Entity（目標を達成するためにやること）
 */
@Data
public class ActionPlan {
	private Integer id;
	private Integer goal_id;
	private String title;
	private int priority; // 表示順。小さいほど上。ドラッグ&ドロップで書き換わる
	private String status; // not_started / in_progress / done
	private Timestamp created_at;
	private int delete_flg;
	private Integer user_id;

	// DB列ではなく、ActionPlanServiceが紐づくメモの実効進捗から算出して詰める値。
	// 集計対象のメモが1件もない場合はnull（"未設定"）
	private Double progress;
}
