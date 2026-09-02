package com.udemy.hello.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

	@Autowired
	private PlanService planService;

	@Autowired
	private NoteService noteService;

	// 「アカウントデータの削除」（目標・プラン・メモを全削除し、アカウント自体は残す）。
	// 論理削除（delete_flg=1）なのでusersテーブルには触れず、ログイン自体は引き続きできる。
	// 両方のUPDATEが途中で失敗した場合に片方だけ消えた状態を残さないよう1トランザクションにする
	@Transactional
	public void deleteAllUserData(int userId) {
		planService.deleteAllForUser(userId);
		noteService.deleteAllForUser(userId);
	}
}
