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

package org.elasticspring.jdbc.datasource.support;

import java.util.Collections;
import java.util.Map;

/**
 * Simple implementation that holds statically all information for the database platform.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class StaticDatabasePlatformSupport extends MapBasedDatabasePlatformSupport {

	private final Map<DatabaseType, String> driverClassNameMappings;
	private final Map<DatabaseType, String> schemeNames;

	/**
	 * Populates both the {@link #driverClassNameMappings} and {@link #schemeNames} with the configuration information
	 */
	public StaticDatabasePlatformSupport() {
		this.driverClassNameMappings = Collections.singletonMap(DatabaseType.MYSQL, "com.mysql.jdbc.Driver");
		this.schemeNames = Collections.singletonMap(DatabaseType.MYSQL, "jdbc:mysql");
	}

	@Override
	protected Map<DatabaseType, String> getDriverClassNameMappings() {
		return this.driverClassNameMappings;
	}

	@Override
	protected Map<DatabaseType, String> getSchemeNames() {
		return this.schemeNames;
	}
}