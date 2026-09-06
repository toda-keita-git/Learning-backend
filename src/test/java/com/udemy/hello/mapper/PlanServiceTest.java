package com.udemy.hello.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.udemy.hello.model.Plan;

// 再評価レポートで指摘された「他ユーザーのプランidを親として直接指定できる」点の回帰防止用。
// DB接続なしで検証できるよう、PlanMapperをモックしてJava側の検証ロジックだけをテストする
class PlanServiceTest {

	private PlanMapper planMapper;
	private PlanService planService;

	private static final int OWNER = 1;
	private static final int OTHER_USER = 2;
	private static final int OWNER_PLAN_ID = 10;
	private static final int OTHER_USER_PLAN_ID = 99;

	@BeforeEach
	void setUp() {
		planMapper = mock(PlanMapper.class);
		planService = new PlanService();
		ReflectionTestUtils.setField(planService, "planMapper", planMapper);
	}

	private Plan ownerPlan(int id, Integer parentId) {
		Plan p = new Plan();
		p.setId(id);
		p.setParent_id(parentId);
		p.setUser_id(OWNER);
		return p;
	}

	@Test
	void insert_他ユーザーのプランIdを親に指定すると拒否する() {
		when(planMapper.existsForUser(OTHER_USER_PLAN_ID, OWNER)).thenReturn(0);

		Plan newPlan = new Plan();
		newPlan.setParent_id(OTHER_USER_PLAN_ID);
		newPlan.setUser_id(OWNER);

		assertThrowsBadRequest(() -> planService.insert(newPlan));
		verify(planMapper, never()).insert(newPlan);
	}

	@Test
	void insert_本人自身のプランIdを親に指定すると成功する() {
		when(planMapper.existsForUser(OWNER_PLAN_ID, OWNER)).thenReturn(1);
		when(planMapper.nextSortOrder(OWNER_PLAN_ID, OWNER)).thenReturn(0);
		when(planMapper.insert(org.mockito.ArgumentMatchers.any())).thenReturn(1);

		Plan newPlan = new Plan();
		newPlan.setParent_id(OWNER_PLAN_ID);
		newPlan.setUser_id(OWNER);

		int result = planService.insert(newPlan);

		assertEquals(1, result);
		verify(planMapper).insert(newPlan);
	}

	@Test
	void insert_ルート直下ならparent_idの存在確認をしない() {
		when(planMapper.nextSortOrder(null, OWNER)).thenReturn(0);
		when(planMapper.insert(org.mockito.ArgumentMatchers.any())).thenReturn(1);

		Plan newPlan = new Plan();
		newPlan.setParent_id(null);
		newPlan.setUser_id(OWNER);

		planService.insert(newPlan);

		verify(planMapper, never()).existsForUser(anyInt(), anyInt());
	}

	@Test
	void reparent_他ユーザーのプランIdを親にはできない() {
		when(planMapper.findAll(OWNER)).thenReturn(List.of(ownerPlan(OWNER_PLAN_ID, null)));

		boolean ok = planService.reparent(OWNER_PLAN_ID, OTHER_USER_PLAN_ID, OWNER);

		assertFalse(ok);
		verify(planMapper, never()).updateParent(anyInt(), anyInt(), anyInt(), anyInt());
	}

	@Test
	void reparent_本人自身のプランへの付け替えは成功する() {
		Plan child = ownerPlan(20, null);
		Plan newParent = ownerPlan(OWNER_PLAN_ID, null);
		when(planMapper.findAll(OWNER)).thenReturn(List.of(child, newParent));
		when(planMapper.nextSortOrder(OWNER_PLAN_ID, OWNER)).thenReturn(0);
		when(planMapper.updateParent(20, OWNER_PLAN_ID, 0, OWNER)).thenReturn(1);

		boolean ok = planService.reparent(20, OWNER_PLAN_ID, OWNER);

		assertTrue(ok);
	}

	private void assertThrowsBadRequest(Runnable runnable) {
		try {
			runnable.run();
		} catch (ResponseStatusException e) {
			assertEquals(400, e.getStatusCode().value());
			return;
		}
		throw new AssertionError("ResponseStatusExceptionが発生しなかった");
	}
}
