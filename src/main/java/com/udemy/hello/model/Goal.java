package com.udemy.hello.model;

import java.sql.Timestamp;

import lombok.Data;

/**
 * 目標（最終目標）Entity
 */
@Data
public class Goal {
	private Integer id;
	private String title;
	private String description;
	private String status; // in_progress / achieved / suspended
	private Timestamp created_at;
	private int delete_flg;
	private Integer user_id;

	// DB列ではなく、GoalServiceが配下のアクションプランの達成率から算出して詰める値。
	// 集計対象のアクションプランが1件もない場合はnull（"未設定"。0%とは区別する）
	private Double progress;
}
