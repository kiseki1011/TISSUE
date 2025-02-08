package com.tissue.support.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseCleaner {

	private final EntityManager entityManager;

	private List<String> tableNames;

	@PostConstruct
	public void extractTableNames() {
		Session session = entityManager.unwrap(Session.class);
		tableNames = new ArrayList<>();

		session.doWork(connection -> {
			ResultSet rs = connection.getMetaData()
				.getTables(null, "PUBLIC", null, new String[] {"TABLE"});

			while (rs.next()) {
				tableNames.add(rs.getString("TABLE_NAME"));
			}
			log.info("tableNames = {}", tableNames);
		});
	}

	@Transactional
	public void execute() {
		entityManager.flush();
		entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

		for (String tableName : tableNames) {
			log.info("TRUNCATE TABLE {}", tableName);
			entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();

			List<String> idColumnNames = getIdColumnNamesForTable(tableName);
			for (String idColumnName : idColumnNames) {
				log.info("ALTER TABLE {} ALTER COLUMN {} RESTART WITH 1", tableName, idColumnName);
				entityManager.createNativeQuery(
						"ALTER TABLE " + tableName + " ALTER COLUMN " + idColumnName + " RESTART WITH 1")
					.executeUpdate();
			}
		}

		entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
	}

	private List<String> getIdColumnNamesForTable(String tableName) {

		List<String> idColumns = new ArrayList<>();
		Session session = entityManager.unwrap(Session.class);

		session.doWork(connection -> {
			try (ResultSet rs = connection.getMetaData().getColumns(null, null, tableName, null)) {
				while (rs.next()) {
					String columnName = rs.getString("COLUMN_NAME");
					if (columnName.equalsIgnoreCase("ID") || columnName.equalsIgnoreCase(tableName + "_ID")) {
						idColumns.add(columnName);
					}
				}
			} catch (SQLException e) {
				log.error("Error while retrieving ID column names for table: {}", tableName, e);
			}
		});

		return idColumns;
	}
}
