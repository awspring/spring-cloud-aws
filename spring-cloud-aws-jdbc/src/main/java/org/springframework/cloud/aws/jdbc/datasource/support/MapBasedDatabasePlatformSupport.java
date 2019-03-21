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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * A map based implementation of the {@link DatabasePlatformSupport} interface. Sub
 * classes must only provide map with the database specific information. This class takes
 * care of asserting and computing the returned information.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public abstract class MapBasedDatabasePlatformSupport implements DatabasePlatformSupport {

	/**
	 * Returns the driver class for the database platform.
	 * @param databaseType - The database type used to lookup the driver class name. Must
	 * not be null
	 * @return - The driver class name, is never null
	 * @throws IllegalArgumentException if there is no driver class name available for the
	 * DatabaseType
	 */
	@Override
	public String getDriverClassNameForDatabase(DatabaseType databaseType) {
		Assert.notNull(databaseType, "databaseType must not be null");
		String candidate = this.getDriverClassNameMappings().get(databaseType);
		Assert.notNull(candidate, String.format(
				"No driver class name found for database :'%s'", databaseType.name()));
		return candidate;
	}

	/**
	 * Constructs the URL for the database by using a {@link URI} to construct the URL.
	 * @param databaseType - The databaseType for which the URL should be constructed.
	 * @param hostname - The hostname without any port information used to connect to.
	 * @param port - The port used to connect to the database
	 * @param databaseName - The database name used to connect to. The usage is
	 * implementation specific (e.g. for Oracle this is the SID)
	 * @return - the database specific URL
	 * @throws IllegalArgumentException if there is no scheme available for the database
	 * type or if the information is not valid to construct a URL.
	 */
	@Override
	public String getDatabaseUrlForDatabase(DatabaseType databaseType, String hostname,
			int port, String databaseName) {
		String scheme = this.getSchemeNames().get(databaseType);
		String authenticationInfo = this.getAuthenticationInfo().get(databaseType);
		Assert.notNull(databaseType, String
				.format("No scheme name found for database :'%s'", databaseType.name()));
		try {
			return new URI(scheme, authenticationInfo, hostname, port,
					databaseName != null ? "/" + databaseName : null, null, null)
							.toString();
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException(
					"Error constructing URI from Host:'" + hostname + "' and port:'"
							+ port + "' and database name:'" + databaseName + "'!");
		}
	}

	/**
	 * Template method that must be implemented in order to retrieve all driver class
	 * names for every supported database platform.
	 * @return Map containing the driver class name for every database platform
	 */
	protected abstract Map<DatabaseType, String> getDriverClassNameMappings();

	/**
	 * Template method that mus be implemented to get all scheme names for every supported
	 * database platform. Scheme names are unfortunately only standardized in the root
	 * scheme (jdbc:) but not in the sub-scheme which is database platform specific.
	 * @return Map containing the schema (and sub-scheme) names for every support database
	 * platform
	 */
	protected abstract Map<DatabaseType, String> getSchemeNames();

	protected abstract Map<DatabaseType, String> getAuthenticationInfo();

}
