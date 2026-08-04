package com.udemy.hello.model;

import lombok.Data;

/**
 * メモに複数持たせられる添付（画像 or コードファイル）。
 * 実体はユーザーのGitHubリポジトリに保存し、ここではパスだけを持つ。
 */
@Data
public class NoteAttachment {
	private Integer id;
	private Integer note_id;
	private String kind; // 'image' / 'code'
	private String github_path;
	private String commit_sha;
	private String repo_name;
	private int sort_order;
}
