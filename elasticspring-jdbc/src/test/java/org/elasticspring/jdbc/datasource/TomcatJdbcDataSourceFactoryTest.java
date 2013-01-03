/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.jdbc.datasource;

import org.junit.Assert;
import org.junit.Test;

import javax.sql.DataSource;

/**
 *
 */
public class TomcatJdbcDataSourceFactoryTest {


	@Test
	public void testCreateWithDefaultSettings() throws Exception {
		TomcatJdbcDataSourceFactory tomcatJdbcDataSourceFactory = new TomcatJdbcDataSourceFactory();

		DataSourceInformation dataSourceInformation = new DataSourceInformation(DataSourceInformation.DatabaseType.MYSQL, "localhost", 3306, "test", "user", "password");
		DataSource dataSource = tomcatJdbcDataSourceFactory.createDataSource(dataSourceInformation);
		Assert.assertNotNull(dataSource);


	}
}
