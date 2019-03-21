/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.jdbc;

import java.sql.Timestamp;
import java.util.Date;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Agim Emruli
 */
@Service
public class SimpleDatabaseService implements DatabaseService {

	private final JdbcTemplate jdbcTemplate;

	@SuppressWarnings("SpringJavaAutowiredMembersInspection")
	@Autowired
	public SimpleDatabaseService(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	@Transactional(readOnly = true)
	public Date getLastUpdate(Date lastAccessDatabase) {
		return this.jdbcTemplate.queryForObject(
				"SELECT lastTest FROM INTEGRATION_TEST WHERE lastTest = ?",
				Timestamp.class, lastAccessDatabase);
	}

	@Transactional
	@Override
	public Date updateLastAccessDatabase() {
		this.jdbcTemplate.update("DROP TABLE IF EXISTS INTEGRATION_TEST");
		this.jdbcTemplate.update(
				"CREATE TABLE IF NOT EXISTS INTEGRATION_TEST(lastTest timestamp(3))");
		Date date = new Date();
		this.jdbcTemplate.update("INSERT INTO INTEGRATION_TEST(lastTest) VALUES(?)",
				date);
		return this.jdbcTemplate.queryForObject(
				"SELECT lastTest FROM INTEGRATION_TEST WHERE lastTest = ?", Date.class,
				date);
	}

}
