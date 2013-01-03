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

import org.springframework.util.Assert;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 *
 */
public abstract class MapBasedDatabasePlatformSupport implements DatabasePlatformSupport {

	@Override
	public String getDriverClassNameForDatabase(DataSourceInformation.DatabaseType databaseType) {
		Assert.notNull(databaseType, "databaseType must not be null");
		String candidate = this.getDriverClassNameMappings().get(databaseType);
		Assert.notNull(candidate, String.format("No driver class name found for database :'%s'", databaseType.name()));
		return candidate;
	}

	@Override
	public String getDatabaseUrlForDatabase(DataSourceInformation.DatabaseType databaseType, String hostname, int port, String databaseName) {
		String scheme = this.getSchemeNames().get(databaseType);
		Assert.notNull(databaseType, String.format("No scheme name found for database :'%s'", databaseType.name()));
		try {
			return new URI(scheme, null, hostname, port, "/" + databaseName, null, null).toString();
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Error constructing URI from Host:'" + hostname + "' and port:'" +
					port + "' and database name:'" + databaseName + "'!");
		}
	}

	protected abstract Map<DataSourceInformation.DatabaseType, String> getDriverClassNameMappings();

	protected abstract Map<DataSourceInformation.DatabaseType, String> getSchemeNames();
}
