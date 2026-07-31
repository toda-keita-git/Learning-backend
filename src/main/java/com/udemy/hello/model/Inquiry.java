package com.udemy.hello.model;

import java.sql.Timestamp;

import lombok.Data;

/**
 * サイト訪問者からの「お問い合わせ」1件分
 * 未ログインの一般訪問者からの送信もあるため、user_idとは紐づけない
 */
@Data
public class Inquiry {
	private Integer id;
	private String name;
	private String email;
	private String message;
	private String status;
	private Timestamp created_at;
}
