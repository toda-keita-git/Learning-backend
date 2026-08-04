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
	// 検索結果一覧のプレビューに表示する見出し。未設定ならexplanatory_textの
	// 先頭部分がフロント側で代わりに表示される
	private String heading_text;
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
	// 添付ファイルがどのリポジトリのものかを記録する。ユーザーが後で使用リポジトリを
	// 切り替えても、過去の記録の添付リンクが指すリポジトリがわかるようにするため
	private String repo_name;
	private Integer user_id;
}
