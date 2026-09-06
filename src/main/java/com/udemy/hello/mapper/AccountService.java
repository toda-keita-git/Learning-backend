package com.udemy.hello.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udemy.hello.model.User;

@Service
public class AccountService {

	@Autowired
	private PlanService planService;

	@Autowired
	private NoteService noteService;

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private GitHubAuthService gitHubAuthService;

	@Autowired
	private GoogleAuthService googleAuthService;

	// 「アカウントデータの削除」（目標・プラン・メモを全削除し、アカウント自体は残す）。
	// 論理削除（delete_flg=1）なのでusersテーブルには触れず、ログイン自体は引き続きできる。
	// 両方のUPDATEが途中で失敗した場合に片方だけ消えた状態を残さないよう1トランザクションにする
	@Transactional
	public void deleteAllUserData(int userId) {
		planService.deleteAllForUser(userId);
		noteService.deleteAllForUser(userId);
	}

	// 「アカウント自体の削除」。データ削除に加えて、GitHub/Googleのトークンを可能な範囲で
	// 先方でも失効させたうえで、users行自体を論理削除し連携情報を消す。
	// トークン失効は外部APIへのHTTP呼び出しのため、DBのトランザクションには含めない
	// （長時間ロックを避けるためと、外部APIの成否に関わらずアカウント削除自体は完了させたいため）
	public void deleteAccount(int userId) {
		User user = userMapper.findById(userId);
		if (user == null) {
			return;
		}
		deleteAllUserData(userId);
		gitHubAuthService.revokeToken(user.getAccessToken());
		googleAuthService.revokeToken(user.getGoogleRefreshToken());
		userMapper.deleteAccount(userId);
	}
}
