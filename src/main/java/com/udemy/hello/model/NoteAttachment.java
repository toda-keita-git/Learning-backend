package com.udemy.hello.model;

import lombok.Data;

/**
 * メモに複数持たせられる添付（画像 or コードファイル）。
 * 実体はユーザーのGitHubリポジトリ、またはGoogleドライブに保存し、ここではパスだけを持つ。
 * providerが"google"の場合、github_pathにはDriveのfileIdを、repo_nameにはdrive_folder_idを
 * 流用して保存する（カラム名はGitHub由来のままだが、リネームによる影響範囲拡大を避けるための
 * 意図的な流用）。
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
	// 'github' / 'google'。リクエストJSONにproviderキーが無い場合（未対応の古いクライアント等）
	// でもNULL挿入でDBのDEFAULT句が無効化されないよう、Java側でも既定値を持たせておく
	private String provider = "github";
}
