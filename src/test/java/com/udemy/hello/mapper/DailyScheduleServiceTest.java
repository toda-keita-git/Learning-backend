package com.udemy.hello.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.udemy.hello.model.DailyScheduleItem;

// 1日時間割機能の検証ロジック（時刻範囲・許可リスト・紐づけ先の所有者確認）を、
// DB接続なしでテストする。3d_daily_schedule_improvement_planの「セキュリティ規則」
// （user_idはJWTから取得、plan_id/note_idは所有者確認後に紐づけ）に対応する
class DailyScheduleServiceTest {

	private DailyScheduleMapper dailyScheduleMapper;
	private PlanMapper planMapper;
	private NoteMapper noteMapper;
	private DailyScheduleService service;

	private static final int OWNER = 1;

	@BeforeEach
	void setUp() {
		dailyScheduleMapper = mock(DailyScheduleMapper.class);
		planMapper = mock(PlanMapper.class);
		noteMapper = mock(NoteMapper.class);
		service = new DailyScheduleService();
		ReflectionTestUtils.setField(service, "dailyScheduleMapper", dailyScheduleMapper);
		ReflectionTestUtils.setField(service, "planMapper", planMapper);
		ReflectionTestUtils.setField(service, "noteMapper", noteMapper);
	}

	private DailyScheduleItem baseItem() {
		DailyScheduleItem item = new DailyScheduleItem();
		item.setUser_id(OWNER);
		item.setSchedule_date(LocalDate.of(2026, 9, 7));
		item.setStart_minute(540); // 9:00
		item.setEnd_minute(600); // 10:00
		item.setSource_type("custom");
		item.setTitle_snapshot("読書");
		return item;
	}

	@Test
	void insert_開始が終了以降なら拒否する() {
		DailyScheduleItem item = baseItem();
		item.setStart_minute(600);
		item.setEnd_minute(600);

		assertThrowsBadRequest(() -> service.insert(item));
		verify(dailyScheduleMapper, never()).insert(any());
	}

	@Test
	void insert_範囲外の時刻は拒否する() {
		DailyScheduleItem item = baseItem();
		item.setEnd_minute(1500);

		assertThrowsBadRequest(() -> service.insert(item));
	}

	@Test
	void insert_不正なsource_typeは拒否する() {
		DailyScheduleItem item = baseItem();
		item.setSource_type("unknown");

		assertThrowsBadRequest(() -> service.insert(item));
	}

	@Test
	void insert_不正なcolor_keyは拒否する() {
		DailyScheduleItem item = baseItem();
		item.setColor_key("rainbow");

		assertThrowsBadRequest(() -> service.insert(item));
	}

	@Test
	void insert_customならsource_idは常にnullになる() {
		DailyScheduleItem item = baseItem();
		item.setSource_id(999); // customなのに紛れ込んだ値は無視されるべき

		service.insert(item);

		assertNull(item.getSource_id());
		verify(dailyScheduleMapper).insert(item);
	}

	@Test
	void insert_プラン紐づけで他ユーザーのプランなら拒否する() {
		DailyScheduleItem item = baseItem();
		item.setSource_type("plan");
		item.setSource_id(10);
		when(planMapper.existsForUser(10, OWNER)).thenReturn(0);

		assertThrowsBadRequest(() -> service.insert(item));
		verify(dailyScheduleMapper, never()).insert(any());
	}

	@Test
	void insert_プラン紐づけで本人のプランなら成功する() {
		DailyScheduleItem item = baseItem();
		item.setSource_type("plan");
		item.setSource_id(10);
		when(planMapper.existsForUser(10, OWNER)).thenReturn(1);

		service.insert(item);

		verify(dailyScheduleMapper).insert(item);
	}

	@Test
	void insert_habit紐づけはnotesを所有者確認する() {
		DailyScheduleItem item = baseItem();
		item.setSource_type("habit");
		item.setSource_id(20);
		when(noteMapper.existsForUser(20, OWNER)).thenReturn(1);

		service.insert(item);

		verify(noteMapper).existsForUser(20, OWNER);
		verify(dailyScheduleMapper).insert(item);
	}

	@Test
	void insert_note紐づけで他ユーザーのメモなら拒否する() {
		DailyScheduleItem item = baseItem();
		item.setSource_type("note");
		item.setSource_id(30);
		when(noteMapper.existsForUser(30, OWNER)).thenReturn(0);

		assertThrowsBadRequest(() -> service.insert(item));
	}

	@Test
	void insert_statusを省略するとplannedになる() {
		DailyScheduleItem item = baseItem();
		item.setStatus(null);

		service.insert(item);

		assertEquals("planned", item.getStatus());
	}

	@Test
	void updateStatus_不正な値は拒否する() {
		assertThrowsBadRequest(() -> service.updateStatus(1L, "cancelled", OWNER));
		verify(dailyScheduleMapper, never()).updateStatus(anyInt(), any(), anyInt());
	}

	@Test
	void updateStatus_正しい値は委譲する() {
		when(dailyScheduleMapper.updateStatus(1L, "done", OWNER)).thenReturn(1);

		boolean result = service.updateStatus(1L, "done", OWNER);

		assertTrue(result);
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
