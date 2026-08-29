package com.udemy.hello.Bean;

import java.util.List;

import com.udemy.hello.model.Note;
import com.udemy.hello.model.Plan;

import lombok.Data;

/**
 * ゲストモード（localStorageのみで完結する未ログインお試し）で作成したプラン・メモを、
 * ログイン後のアカウントへ取り込むためのリクエスト本体。
 *
 * plans[].id / plans[].parent_id、notes[].links の中身は、いずれもゲスト側の
 * localStorageで振られたローカルID（実DBには存在しない値）。
 * GuestImportServiceがこれらのローカルIDだけを手がかりに親子関係・リンクを
 * 新しく採番されたIDへ張り直す。
 */
@Data
public class GuestImportRequest {
	private List<Plan> plans;
	private List<Note> notes;
}
