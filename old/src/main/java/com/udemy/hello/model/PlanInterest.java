package com.udemy.hello.model;

import java.sql.Timestamp;

import lombok.Data;

/**
 * Proプランの「通知を希望する」を押したユーザーの記録
 */
@Data
public class PlanInterest {
	private Integer id;
	private Integer user_id;
	private String github_login;
	private Timestamp created_at;
}
