package com.udemy.hello.mapper.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import com.udemy.hello.mapper.PlanMapper;
import com.udemy.hello.model.Plan;

/**
 * PlanMapper.xml（本番と同じファイル）を実際にH2上で実行し、
 * `14-reassessment-security-fixes` で追加した所有者検証SQL（existsForUser）と、
 * `findAll` の delete_flg / user_id 絞り込みを、Mockitoでは検出できない
 * SQLの書き間違いレベルで確認する。
 */
class PlanMapperOwnershipSqlIT {

	private static final int OWNER = 1;
	private static final int OTHER_USER = 2;

	private SqlSessionFactory factory;

	@BeforeEach
	void setUp() throws Exception {
		factory = H2MapperTestSupport.buildSqlSessionFactory("mapper/PlanMapper.xml", PlanMapper.class, Plan.class);
		H2MapperTestSupport.execute(factory,
				"CREATE TABLE plans (" +
						"id SERIAL PRIMARY KEY," +
						"user_id INT NOT NULL," +
						"parent_id INT," +
						"title VARCHAR(255) NOT NULL," +
						"description TEXT," +
						"status VARCHAR(20) NOT NULL DEFAULT 'not_started'," +
						"start_date DATE," +
						"due_date DATE," +
						"sort_order INT NOT NULL DEFAULT 0," +
						"delete_flg INT NOT NULL DEFAULT 0," +
						"created_at TIMESTAMP NOT NULL DEFAULT now()" +
						")");
	}

	private int insertPlan(SqlSession session, int userId, Integer parentId, int deleteFlg) {
		session.getConnection();
		try (var stmt = session.getConnection().prepareStatement(
				"INSERT INTO plans (user_id, parent_id, title, delete_flg) VALUES (?, ?, 'x', ?)",
				java.sql.Statement.RETURN_GENERATED_KEYS)) {
			stmt.setInt(1, userId);
			if (parentId == null) {
				stmt.setNull(2, java.sql.Types.INTEGER);
			} else {
				stmt.setInt(2, parentId);
			}
			stmt.setInt(3, deleteFlg);
			stmt.executeUpdate();
			try (var keys = stmt.getGeneratedKeys()) {
				keys.next();
				return keys.getInt(1);
			}
		} catch (java.sql.SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void existsForUser_本人の未削除プランは1を返す() {
		try (SqlSession session = factory.openSession(true)) {
			PlanMapper mapper = session.getMapper(PlanMapper.class);
			int planId = insertPlan(session, OWNER, null, 0);

			assertEquals(1, mapper.existsForUser(planId, OWNER));
		}
	}

	@Test
	void existsForUser_他ユーザーのプランは0を返す() {
		try (SqlSession session = factory.openSession(true)) {
			PlanMapper mapper = session.getMapper(PlanMapper.class);
			int otherUsersPlanId = insertPlan(session, OTHER_USER, null, 0);

			assertEquals(0, mapper.existsForUser(otherUsersPlanId, OWNER));
		}
	}

	@Test
	void existsForUser_本人でも削除済みプランは0を返す() {
		try (SqlSession session = factory.openSession(true)) {
			PlanMapper mapper = session.getMapper(PlanMapper.class);
			int deletedPlanId = insertPlan(session, OWNER, null, 1);

			assertEquals(0, mapper.existsForUser(deletedPlanId, OWNER));
		}
	}

	@Test
	void existsForUser_存在しないidは0を返す() {
		try (SqlSession session = factory.openSession(true)) {
			PlanMapper mapper = session.getMapper(PlanMapper.class);

			assertEquals(0, mapper.existsForUser(999999, OWNER));
		}
	}

	@Test
	void findAll_他ユーザーや削除済みは含まれない() {
		try (SqlSession session = factory.openSession(true)) {
			PlanMapper mapper = session.getMapper(PlanMapper.class);
			int ownPlan = insertPlan(session, OWNER, null, 0);
			insertPlan(session, OWNER, null, 1); // 削除済み（含まれてはいけない）
			insertPlan(session, OTHER_USER, null, 0); // 他ユーザー（含まれてはいけない）

			List<Plan> result = mapper.findAll(OWNER);

			assertEquals(1, result.size());
			assertEquals(ownPlan, result.get(0).getId());
		}
	}

	@Test
	void updateParent_SQL自体はparent_idの所有者を検証しない_所有者チェックはJava側の責務であることの確認() {
		// updateParentのWHEREはid+user_idのみで、書き込む値(parent_id)自体の所有者は
		// SQLレベルでは検証していない（意図的な設計。所有者チェックはPlanService.reparentが
		// existsForUser等で事前に行う）。この前提が崩れて「SQL側でも安全」という誤解が
		// 生まれないよう、現状の挙動をテストとして残しておく
		try (SqlSession session = factory.openSession(true)) {
			PlanMapper mapper = session.getMapper(PlanMapper.class);
			int myPlan = insertPlan(session, OWNER, null, 0);
			int othersPlan = insertPlan(session, OTHER_USER, null, 0);

			int updated = mapper.updateParent(myPlan, othersPlan, 0, OWNER);

			assertEquals(1, updated, "id+user_idが一致するため更新自体は成功する");
			Plan reloaded = mapper.findAll(OWNER).stream().filter(p -> p.getId() == myPlan).findFirst().orElseThrow();
			assertTrue(othersPlan == reloaded.getParent_id(),
					"parent_idの値そのものはSQLでは検証されない＝呼び出し元(PlanService)の事前検証が必須");
		}
	}
}
