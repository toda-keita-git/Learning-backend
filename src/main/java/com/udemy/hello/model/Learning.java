package com.udemy.hello.model;

import java.sql.Timestamp;
import java.util.Date;

import lombok.Data;

/**
 * ユーザー情報 Entity
 */
@Data
public class Learning {
	private Integer id;
	private String title;
	private String explanatory_text;
	private Integer understanding_level;
	private String reference_url;
	private Timestamp created_at;
	private String category_name;
	private int category_id;
	private int delete_flg;
	private String[] tags;
	private String github_path;
	private String commit_sha;
}
