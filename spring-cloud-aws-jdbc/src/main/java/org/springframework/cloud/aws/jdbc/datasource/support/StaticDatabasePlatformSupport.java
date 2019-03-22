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
import java.util.HashMap;
import java.util.Map;

/**
 * Simple implementation that holds statically all information for the database platform.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class StaticDatabasePlatformSupport extends MapBasedDatabasePlatformSupport {

	private static final String JDBC_SCHEME_NAME = "jdbc:";

	private final Map<DatabaseType, String> driverClassNameMappings;

	private final Map<DatabaseType, String> schemeNames;

	/**
	 * Populates both the {@link #driverClassNameMappings} and {@link #schemeNames} with
	 * the configuration information.
	 */
	public StaticDatabasePlatformSupport() {
		this.driverClassNameMappings = getDefaultDriverClassNameMappings();
		this.schemeNames = getDefaultSchemeNames();
	}

	private static Map<DatabaseType, String> getDefaultDriverClassNameMappings() {
		HashMap<DatabaseType, String> driverClassNameMappings = new HashMap<>();
		driverClassNameMappings.put(DatabaseType.MYSQL, "com.mysql.jdbc.Driver");
		driverClassNameMappings.put(DatabaseType.ORACLE, "oracle.jdbc.OracleDriver");
		driverClassNameMappings.put(DatabaseType.SQLSERVER,
				"net.sourceforge.jtds.jdbc.Driver");
		driverClassNameMappings.put(DatabaseType.POSTGRES, "org.postgresql.Driver");
		driverClassNameMappings.put(DatabaseType.MARIA, "org.mariadb.jdbc.Driver");
		driverClassNameMappings.put(DatabaseType.AURORA_POSTGRESQL,
				driverClassNameMappings.get(DatabaseType.POSTGRES));
		driverClassNameMappings.put(DatabaseType.AURORA,
				driverClassNameMappings.get(DatabaseType.MYSQL));
		return Collections.unmodifiableMap(driverClassNameMappings);
	}

	private static Map<DatabaseType, String> getDefaultSchemeNames() {
		HashMap<DatabaseType, String> schemeNamesMappings = new HashMap<>();
		schemeNamesMappings.put(DatabaseType.MYSQL, JDBC_SCHEME_NAME + "mysql");
		schemeNamesMappings.put(DatabaseType.ORACLE, JDBC_SCHEME_NAME + "oracle:thin");
		schemeNamesMappings.put(DatabaseType.SQLSERVER,
				JDBC_SCHEME_NAME + "jtds:sqlserver");
		schemeNamesMappings.put(DatabaseType.POSTGRES, JDBC_SCHEME_NAME + "postgresql");
		schemeNamesMappings.put(DatabaseType.MARIA, JDBC_SCHEME_NAME + "mariadb");
		schemeNamesMappings.put(DatabaseType.AURORA_POSTGRESQL,
				schemeNamesMappings.get(DatabaseType.POSTGRES));
		schemeNamesMappings.put(DatabaseType.AURORA,
				schemeNamesMappings.get(DatabaseType.MYSQL));
		return Collections.unmodifiableMap(schemeNamesMappings);
	}

	@Override
	protected Map<DatabaseType, String> getDriverClassNameMappings() {
		return this.driverClassNameMappings;
	}

	@Override
	protected Map<DatabaseType, String> getSchemeNames() {
		return this.schemeNames;
	}

	@Override
	protected Map<DatabaseType, String> getAuthenticationInfo() {
		return Collections.singletonMap(DatabaseType.ORACLE, "@");
	}

}
