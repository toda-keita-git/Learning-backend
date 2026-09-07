package com.udemy.hello.mapper.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.udemy.hello.mapper.NoteMapper;
import com.udemy.hello.model.Note;
import com.udemy.hello.model.NoteAttachment;
import com.udemy.hello.model.NotePlanLink;
import com.udemy.hello.model.NoteTagName;
import com.udemy.hello.model.NoteTodoItem;

/**
 * NoteMapper.xml（本番と同じファイル）を実際にH2上で実行し、
 * `14-reassessment-security-fixes` で追加した insertLink / findLinksByUser の
 * 所有者検証SQLを、Mockitoでは検出できないSQLの書き間違いレベルで確認する。
 */
class NoteMapperOwnershipSqlIT {

	private static final int OWNER = 1;
	private static final int OTHER_USER = 2;

	private SqlSessionFactory factory;

	@BeforeEach
	void setUp() throws Exception {
		factory = H2MapperTestSupport.buildSqlSessionFactory("mapper/NoteMapper.xml", NoteMapper.class,
				Note.class, NoteTodoItem.class, NoteTagName.class, NotePlanLink.class, NoteAttachment.class);
		H2MapperTestSupport.execute(factory,
				"CREATE TABLE notes (" +
						"id SERIAL PRIMARY KEY," +
						"user_id INT NOT NULL," +
						"delete_flg INT NOT NULL DEFAULT 0" +
						")",
				"CREATE TABLE plans (" +
						"id SERIAL PRIMARY KEY," +
						"user_id INT NOT NULL," +
						"delete_flg INT NOT NULL DEFAULT 0" +
						")",
				"CREATE TABLE note_plan_links (" +
						"id SERIAL PRIMARY KEY," +
						"note_id INT NOT NULL," +
						"plan_id INT NOT NULL," +
						"UNIQUE (note_id, plan_id)" +
						")");
	}

	private int insertRow(String table, int userId, int deleteFlg) throws SQLException {
		try (var conn = factory.getConfiguration().getEnvironment().getDataSource().getConnection();
				var stmt = conn.prepareStatement(
						"INSERT INTO " + table + " (user_id, delete_flg) VALUES (?, ?)",
						Statement.RETURN_GENERATED_KEYS)) {
			stmt.setInt(1, userId);
			stmt.setInt(2, deleteFlg);
			stmt.executeUpdate();
			try (var keys = stmt.getGeneratedKeys()) {
				keys.next();
				return keys.getInt(1);
			}
		}
	}

	// insertLinkを直接呼ぶ代わりに使う。
	//
	// H2（PostgreSQL互換モード）は `ON CONFLICT (列名, ...) DO NOTHING` という、
	// 競合対象の列を明示する構文をパースできない（`ON CONFLICT DO NOTHING` だけなら通る）。
	// これはH2側の対応範囲の限界で、本番のPostgreSQL(NeonDB)では両方とも正しく動く。
	// そのため、テストしたい「所有者検証のWHERE EXISTS句」はXMLから実際に取り出したうえで、
	// H2がパースできない `ON CONFLICT (note_id, plan_id)` の列指定部分だけをテスト用に
	// 取り除いて実行する。取り除くのは競合時の挙動（DO NOTHING）ではなく列指定の有無だけで、
	// 検証したい所有者チェックのSQLはXMLの内容をそのまま使っている
	private void callInsertLink(SqlSession session, int noteId, int planId, int userId) throws SQLException {
		MappedStatement ms = session.getConfiguration()
				.getMappedStatement("com.udemy.hello.mapper.NoteMapper.insertLink");
		Map<String, Object> params = new HashMap<>();
		params.put("note_id", noteId);
		params.put("plan_id", planId);
		params.put("user_id", userId);
		BoundSql boundSql = ms.getBoundSql(params);

		String sql = boundSql.getSql().replace("ON CONFLICT (note_id, plan_id) DO NOTHING", "ON CONFLICT DO NOTHING");
		List<ParameterMapping> mappings = boundSql.getParameterMappings();

		try (var stmt = session.getConnection().prepareStatement(sql)) {
			for (int i = 0; i < mappings.size(); i++) {
				stmt.setObject(i + 1, boundSql.getAdditionalParameter(mappings.get(i).getProperty()) != null
						? boundSql.getAdditionalParameter(mappings.get(i).getProperty())
						: params.get(mappings.get(i).getProperty()));
			}
			stmt.executeUpdate();
		}
	}

	private int linkCount(int noteId, int planId) throws SQLException {
		try (var conn = factory.getConfiguration().getEnvironment().getDataSource().getConnection();
				var stmt = conn.prepareStatement(
						"SELECT COUNT(*) FROM note_plan_links WHERE note_id = ? AND plan_id = ?")) {
			stmt.setInt(1, noteId);
			stmt.setInt(2, planId);
			try (var rs = stmt.executeQuery()) {
				rs.next();
				return rs.getInt(1);
			}
		}
	}

	@Test
	void insertLink_自分のメモと自分のプランならリンクできる() throws SQLException {
		int noteId = insertRow("notes", OWNER, 0);
		int planId = insertRow("plans", OWNER, 0);

		try (SqlSession session = factory.openSession(true)) {
			callInsertLink(session, noteId, planId, OWNER);
		}

		assertEquals(1, linkCount(noteId, planId));
	}

	@Test
	void insertLink_リンク先プランが他ユーザーなら追加されない() throws SQLException {
		int myNote = insertRow("notes", OWNER, 0);
		int othersPlan = insertRow("plans", OTHER_USER, 0);

		try (SqlSession session = factory.openSession(true)) {
			callInsertLink(session, myNote, othersPlan, OWNER);
		}

		assertEquals(0, linkCount(myNote, othersPlan));
	}

	@Test
	void insertLink_メモが他ユーザーなら追加されない() throws SQLException {
		int othersNote = insertRow("notes", OTHER_USER, 0);
		int myPlan = insertRow("plans", OWNER, 0);

		try (SqlSession session = factory.openSession(true)) {
			callInsertLink(session, othersNote, myPlan, OWNER);
		}

		assertEquals(0, linkCount(othersNote, myPlan));
	}

	@Test
	void insertLink_自分のメモでも削除済みなら追加されない() throws SQLException {
		int deletedNote = insertRow("notes", OWNER, 1);
		int myPlan = insertRow("plans", OWNER, 0);

		try (SqlSession session = factory.openSession(true)) {
			callInsertLink(session, deletedNote, myPlan, OWNER);
		}

		assertEquals(0, linkCount(deletedNote, myPlan));
	}

	@Test
	void insertLink_自分のプランでも削除済みなら追加されない() throws SQLException {
		int myNote = insertRow("notes", OWNER, 0);
		int deletedPlan = insertRow("plans", OWNER, 1);

		try (SqlSession session = factory.openSession(true)) {
			callInsertLink(session, myNote, deletedPlan, OWNER);
		}

		assertEquals(0, linkCount(myNote, deletedPlan));
	}

	@Test
	void findLinksByUser_プラン側の所有者が異なる不整合データは返さない() throws SQLException {
		// insertLink経由では作れない組み合わせ（メモは自分、プランは他ユーザー）を、
		// 過去データの不整合を模して直接INSERTし、findLinksByUserが除外することを確認する
		int myNote = insertRow("notes", OWNER, 0);
		int othersPlan = insertRow("plans", OTHER_USER, 0);
		try (var conn = factory.getConfiguration().getEnvironment().getDataSource().getConnection();
				var stmt = conn.prepareStatement("INSERT INTO note_plan_links (note_id, plan_id) VALUES (?, ?)")) {
			stmt.setInt(1, myNote);
			stmt.setInt(2, othersPlan);
			stmt.executeUpdate();
		}

		try (SqlSession session = factory.openSession(true)) {
			NoteMapper mapper = session.getMapper(NoteMapper.class);
			List<NotePlanLink> ownerLinks = mapper.findLinksByUser(OWNER);
			List<NotePlanLink> otherLinks = mapper.findLinksByUser(OTHER_USER);

			assertTrue(ownerLinks.isEmpty(), "自分のメモ×他ユーザーのプランは自分の一覧にも出てはいけない");
			assertTrue(otherLinks.isEmpty(), "自分のメモ×他ユーザーのプランは相手の一覧にも出てはいけない（メモは自分のものなので）");
		}
	}
}
