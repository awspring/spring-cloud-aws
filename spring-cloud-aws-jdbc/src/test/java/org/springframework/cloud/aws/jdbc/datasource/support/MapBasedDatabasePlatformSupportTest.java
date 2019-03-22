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

package org.springframework.cloud.aws.jdbc.datasource.support;

import java.util.Collections;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test class for
 * {@link org.springframework.cloud.aws.jdbc.datasource.support.MapBasedDatabasePlatformSupport}.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class MapBasedDatabasePlatformSupportTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testGetDriverClassNameForDatabase() throws Exception {
		assertThat(new SimpleDatabasePlatformSupport()
				.getDriverClassNameForDatabase(DatabaseType.MYSQL))
						.isEqualTo("com.mysql.jdbc.Driver");
	}

	@Test
	public void testGetDriverNonConfiguredDatabasePlatform() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("No driver");
		new SimpleDatabasePlatformSupport()
				.getDriverClassNameForDatabase(DatabaseType.ORACLE);
	}

	@Test
	public void testNullDriver() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("must not be null");
		new SimpleDatabasePlatformSupport().getDriverClassNameForDatabase(null);
	}

	@Test
	public void testGetDatabaseUrlForDatabase() throws Exception {
		SimpleDatabasePlatformSupport simpleDatabasePlatformSupport = new SimpleDatabasePlatformSupport();
		String url = simpleDatabasePlatformSupport.getDatabaseUrlForDatabase(
				DatabaseType.MYSQL, "localhost", 3306, "testDb");
		assertThat(url).isEqualTo("jdbc:mysql://localhost:3306/testDb");
	}

	@Test
	public void testGetDatabaseUrlWrongHostName() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException
				.expectMessage("Error constructing URI from Host:'localhost<'");
		SimpleDatabasePlatformSupport simpleDatabasePlatformSupport = new SimpleDatabasePlatformSupport();
		String url = simpleDatabasePlatformSupport.getDatabaseUrlForDatabase(
				DatabaseType.MYSQL, "localhost<", 3306, "testDb");
		assertThat(url).isEqualTo("jdbc:mysql://localhost:3306/testDb");
	}

	private static class SimpleDatabasePlatformSupport
			extends MapBasedDatabasePlatformSupport {

		@Override
		protected Map<DatabaseType, String> getDriverClassNameMappings() {
			return Collections.singletonMap(DatabaseType.MYSQL, "com.mysql.jdbc.Driver");
		}

		@Override
		protected Map<DatabaseType, String> getSchemeNames() {
			return Collections.singletonMap(DatabaseType.MYSQL, "jdbc:mysql");
		}

		@Override
		protected Map<DatabaseType, String> getAuthenticationInfo() {
			return Collections.emptyMap();
		}

	}

}
