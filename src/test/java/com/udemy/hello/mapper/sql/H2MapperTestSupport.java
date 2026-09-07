package com.udemy.hello.mapper.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;

/**
 * 本番と同じ src/main/resources/mapper/*.xml をH2（PostgreSQL互換モード）上で実行し、
 * 所有者検証のSQLそのものを検証するための土台。
 *
 * 本番はPostgreSQL(NeonDB)で、Testcontainers等の実DBを使った検証が理想だが、
 * この環境にはDockerデーモンが無く（`docker info`がsocket接続エラー）使えない。
 * Mockitoでマッパーをモックする通常のユニットテスト（PlanServiceTest等）は
 * Javaの分岐ロジックしか検証できず、WHERE句の書き間違い自体は検出できないため、
 * H2をPostgreSQL互換モードで動かし、本番のXMLファイルをそのまま読み込んで実行する。
 */
final class H2MapperTestSupport {

	private H2MapperTestSupport() {
	}

	static SqlSessionFactory buildSqlSessionFactory(String mapperXmlResource, Class<?> mapperInterface,
			Class<?>... typeAliasClasses) throws Exception {
		String dbName = "test_" + UUID.randomUUID().toString().replace("-", "");
		JdbcDataSource dataSource = new JdbcDataSource();
		// MODE=PostgreSQL: ON CONFLICT / NULLS FIRST 等、本番XMLのPostgreSQL構文をH2でも解釈させる。
		// DB_CLOSE_DELAY=-1: 最後の接続が閉じてもテスト終了まではDBを保持する
		dataSource.setUrl("jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
		dataSource.setUser("sa");
		dataSource.setPassword("");

		Environment environment = new Environment("test", new JdbcTransactionFactory(), dataSource);
		Configuration configuration = new Configuration(environment);
		// application.propertiesの mybatis.configuration.map-underscore-to-camel-case=true と揃える
		configuration.setMapUnderscoreToCamelCase(true);
		for (Class<?> aliasClass : typeAliasClasses) {
			configuration.getTypeAliasRegistry().registerAlias(aliasClass);
		}

		// XMLMapperBuilder.parse()は、namespaceと同名のインターフェースが
		// クラスパス上にあれば自動的にMapperRegistryへも登録する（bindMapperForNamespace）。
		// そのためconfiguration.addMapper(mapperInterface)を別途呼ぶと二重登録エラーになる
		try (var is = Resources.getResourceAsStream(mapperXmlResource)) {
			new XMLMapperBuilder(is, configuration, mapperXmlResource, configuration.getSqlFragments()).parse();
		}
		if (!configuration.hasMapper(mapperInterface)) {
			configuration.addMapper(mapperInterface);
		}

		return new SqlSessionFactoryBuilder().build(configuration);
	}

	static void execute(SqlSessionFactory factory, String... ddl) throws SQLException {
		try (Connection conn = factory.getConfiguration().getEnvironment().getDataSource().getConnection();
				Statement stmt = conn.createStatement()) {
			for (String sql : ddl) {
				stmt.execute(sql);
			}
		}
	}
}
