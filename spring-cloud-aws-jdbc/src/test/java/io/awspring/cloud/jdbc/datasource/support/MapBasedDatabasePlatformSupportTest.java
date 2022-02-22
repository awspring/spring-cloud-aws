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

package io.awspring.cloud.jdbc.datasource.support;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test class for
 * {@link io.awspring.cloud.jdbc.datasource.support.MapBasedDatabasePlatformSupport}.
 *
 * @author Agim Emruli
 * @since 1.0
 */
class MapBasedDatabasePlatformSupportTest {

	@Test
	void testGetDriverClassNameForDatabase() throws Exception {
		assertThat(new SimpleDatabasePlatformSupport().getDriverClassNameForDatabase(DatabaseType.MYSQL))
				.isEqualTo("com.mysql.jdbc.Driver");
	}

	@Test
	void testGetDriverNonConfiguredDatabasePlatform() throws Exception {
		assertThatThrownBy(() -> new SimpleDatabasePlatformSupport().getDriverClassNameForDatabase(DatabaseType.ORACLE))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("No driver");
	}

	@Test
	void testNullDriver() throws Exception {
		assertThatThrownBy(() -> new SimpleDatabasePlatformSupport().getDriverClassNameForDatabase(null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must not be null");
	}

	@Test
	void testGetDatabaseUrlForDatabase() throws Exception {
		SimpleDatabasePlatformSupport simpleDatabasePlatformSupport = new SimpleDatabasePlatformSupport();
		String url = simpleDatabasePlatformSupport.getDatabaseUrlForDatabase(DatabaseType.MYSQL, "localhost", 3306,
				"testDb");
		assertThat(url).isEqualTo("jdbc:mysql://localhost:3306/testDb");
	}

	@Test
	void testGetDatabaseUrlWrongHostName() throws Exception {
		SimpleDatabasePlatformSupport simpleDatabasePlatformSupport = new SimpleDatabasePlatformSupport();
		assertThatThrownBy(() -> simpleDatabasePlatformSupport.getDatabaseUrlForDatabase(DatabaseType.MYSQL,
				"localhost<", 3306, "testDb")).isInstanceOf(IllegalArgumentException.class)
						.hasMessageContaining("Error constructing URI from Host:'localhost<'");
	}

	private static class SimpleDatabasePlatformSupport extends MapBasedDatabasePlatformSupport {

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
