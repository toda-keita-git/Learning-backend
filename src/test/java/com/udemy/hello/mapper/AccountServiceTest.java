package com.udemy.hello.mapper;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.udemy.hello.model.User;

// アカウント自体の削除（account_delete）が、データ削除・OAuthトークン失効・
// users行の論理削除を正しい条件で呼び分けることをDB接続なしで検証する
class AccountServiceTest {

	private PlanService planService;
	private NoteService noteService;
	private UserMapper userMapper;
	private GitHubAuthService gitHubAuthService;
	private GoogleAuthService googleAuthService;
	private AccountService accountService;

	private static final int USER_ID = 1;

	@BeforeEach
	void setUp() {
		planService = mock(PlanService.class);
		noteService = mock(NoteService.class);
		userMapper = mock(UserMapper.class);
		gitHubAuthService = mock(GitHubAuthService.class);
		googleAuthService = mock(GoogleAuthService.class);

		accountService = new AccountService();
		ReflectionTestUtils.setField(accountService, "planService", planService);
		ReflectionTestUtils.setField(accountService, "noteService", noteService);
		ReflectionTestUtils.setField(accountService, "userMapper", userMapper);
		ReflectionTestUtils.setField(accountService, "gitHubAuthService", gitHubAuthService);
		ReflectionTestUtils.setField(accountService, "googleAuthService", googleAuthService);
	}

	@Test
	void deleteAccount_存在しないユーザーは何もしない() {
		when(userMapper.findById(USER_ID)).thenReturn(null);

		accountService.deleteAccount(USER_ID);

		verify(planService, never()).deleteAllForUser(USER_ID);
		verify(noteService, never()).deleteAllForUser(USER_ID);
		verify(userMapper, never()).deleteAccount(USER_ID);
	}

	@Test
	void deleteAccount_GitHubのみ連携済みならGitHubのトークンだけ失効させる() {
		User user = new User();
		user.setId(USER_ID);
		user.setAccessToken("gh-token");
		user.setGoogleRefreshToken(null);
		when(userMapper.findById(USER_ID)).thenReturn(user);

		accountService.deleteAccount(USER_ID);

		verify(planService).deleteAllForUser(USER_ID);
		verify(noteService).deleteAllForUser(USER_ID);
		verify(gitHubAuthService).revokeToken(eq("gh-token"));
		verify(googleAuthService).revokeToken(eq((String) null));
		verify(userMapper).deleteAccount(USER_ID);
	}

	@Test
	void deleteAccount_両方連携済みなら両方のトークンを失効させる() {
		User user = new User();
		user.setId(USER_ID);
		user.setAccessToken("gh-token");
		user.setGoogleRefreshToken("google-refresh-token");
		when(userMapper.findById(USER_ID)).thenReturn(user);

		accountService.deleteAccount(USER_ID);

		verify(gitHubAuthService).revokeToken(eq("gh-token"));
		verify(googleAuthService).revokeToken(eq("google-refresh-token"));
		verify(userMapper).deleteAccount(USER_ID);
	}
}
