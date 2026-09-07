package com.udemy.hello.mapper.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.udemy.hello.mapper.DailyScheduleMapper;
import com.udemy.hello.model.DailyScheduleItem;

/**
 * DailyScheduleMapper.xml（本番と同じファイル）を実際にH2上で実行し、
 * update/delete/updateStatusのWHERE句（id+user_id）とfindByUserAndDateの
 * user_id・schedule_date・delete_flg絞り込みを、Mockitoでは検出できない
 * SQLの書き間違いレベルで確認する。
 */
class DailyScheduleMapperOwnershipSqlIT {

	private static final int OWNER = 1;
	private static final int OTHER_USER = 2;
	private static final LocalDate TODAY = LocalDate.of(2026, 9, 7);

	private SqlSessionFactory factory;

	@BeforeEach
	void setUp() throws Exception {
		factory = H2MapperTestSupport.buildSqlSessionFactory("mapper/DailyScheduleMapper.xml",
				DailyScheduleMapper.class, DailyScheduleItem.class);
		H2MapperTestSupport.execute(factory,
				"CREATE TABLE daily_schedule_items (" +
						"id BIGSERIAL PRIMARY KEY," +
						"user_id INT NOT NULL," +
						"schedule_date DATE NOT NULL," +
						"start_minute SMALLINT NOT NULL," +
						"end_minute SMALLINT NOT NULL," +
						"timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Tokyo'," +
						"source_type VARCHAR(16) NOT NULL," +
						"source_id INT," +
						"title_snapshot VARCHAR(255) NOT NULL," +
						"status VARCHAR(16) NOT NULL DEFAULT 'planned'," +
						"color_key VARCHAR(24)," +
						"delete_flg INT NOT NULL DEFAULT 0," +
						"created_at TIMESTAMP NOT NULL DEFAULT now()," +
						"updated_at TIMESTAMP NOT NULL DEFAULT now()" +
						")");
	}

	private DailyScheduleItem newItem(int userId) {
		DailyScheduleItem item = new DailyScheduleItem();
		item.setUser_id(userId);
		item.setSchedule_date(TODAY);
		item.setStart_minute(540);
		item.setEnd_minute(600);
		item.setTimezone("Asia/Tokyo");
		item.setSource_type("custom");
		item.setTitle_snapshot("読書");
		item.setStatus("planned");
		return item;
	}

	@Test
	void findByUserAndDate_他ユーザーや削除済みは含まれない() {
		try (SqlSession session = factory.openSession(true)) {
			DailyScheduleMapper mapper = session.getMapper(DailyScheduleMapper.class);

			DailyScheduleItem mine = newItem(OWNER);
			mapper.insert(mine);

			DailyScheduleItem deleted = newItem(OWNER);
			mapper.insert(deleted);
			mapper.delete(deleted.getId(), OWNER);

			DailyScheduleItem othersItem = newItem(OTHER_USER);
			mapper.insert(othersItem);

			DailyScheduleItem otherDate = newItem(OWNER);
			otherDate.setSchedule_date(TODAY.plusDays(1));
			mapper.insert(otherDate);

			List<DailyScheduleItem> result = mapper.findByUserAndDate(OWNER, TODAY);

			assertEquals(1, result.size());
			assertEquals(mine.getId(), result.get(0).getId());
		}
	}

	@Test
	void update_他ユーザーのidを指定しても対象の行は書き換わらない() {
		try (SqlSession session = factory.openSession(true)) {
			DailyScheduleMapper mapper = session.getMapper(DailyScheduleMapper.class);
			DailyScheduleItem original = newItem(OWNER);
			mapper.insert(original);

			// 他ユーザー（OTHER_USER）になりすまして、本人（OWNER）の予定idを書き換えようとする想定
			DailyScheduleItem attackerAttempt = newItem(OTHER_USER);
			attackerAttempt.setId(original.getId());
			attackerAttempt.setTitle_snapshot("乗っ取り");

			int updated = mapper.update(attackerAttempt);

			assertEquals(0, updated, "id+user_idが一致しないため更新されない");
			DailyScheduleItem reloaded = mapper.findByUserAndDate(OWNER, TODAY).get(0);
			assertEquals("読書", reloaded.getTitle_snapshot(), "本人の行の内容は変化していない");
		}
	}

	@Test
	void updateStatus_他ユーザーのidでは0件のまま() {
		try (SqlSession session = factory.openSession(true)) {
			DailyScheduleMapper mapper = session.getMapper(DailyScheduleMapper.class);
			DailyScheduleItem item = newItem(OWNER);
			mapper.insert(item);

			int result = mapper.updateStatus(item.getId(), "done", OTHER_USER);

			assertEquals(0, result);
		}
	}

	@Test
	void delete_本人の予定は削除フラグが立つ() {
		try (SqlSession session = factory.openSession(true)) {
			DailyScheduleMapper mapper = session.getMapper(DailyScheduleMapper.class);
			DailyScheduleItem item = newItem(OWNER);
			mapper.insert(item);

			int deleted = mapper.delete(item.getId(), OWNER);

			assertEquals(1, deleted);
			assertTrue(mapper.findByUserAndDate(OWNER, TODAY).isEmpty());
		}
	}

	@Test
	void delete_他ユーザーのidでは削除されない() {
		try (SqlSession session = factory.openSession(true)) {
			DailyScheduleMapper mapper = session.getMapper(DailyScheduleMapper.class);
			DailyScheduleItem item = newItem(OWNER);
			mapper.insert(item);

			int deleted = mapper.delete(item.getId(), OTHER_USER);

			assertEquals(0, deleted);
			assertEquals(1, mapper.findByUserAndDate(OWNER, TODAY).size());
		}
	}
}
