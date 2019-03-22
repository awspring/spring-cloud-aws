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

import org.springframework.cloud.aws.core.support.documentation.RuntimeUse;
import org.springframework.util.Assert;

/**
 * Enumeration that holds all supported databases. The enumeration is mainly driven by the
 * supported databases by the underlying AWS cloud implementation.
 * @author Agim Emruli
 */
public enum DatabaseType {

	// @checkstyle:off
	@RuntimeUse
	MYSQL, @RuntimeUse
	ORACLE, @RuntimeUse
	SQLSERVER, @RuntimeUse
	POSTGRES, @RuntimeUse
	MARIA, @RuntimeUse
	AURORA_POSTGRESQL, @RuntimeUse
	AURORA;
	// @checkstyle:on

	public static DatabaseType fromEngine(String engineName) {
		Assert.notNull(engineName, "Engine must not be null");
		String lookupName = engineName.toUpperCase().replace("-", "_");
		for (DatabaseType databaseType : values()) {
			if (lookupName.startsWith(databaseType.toString())) {
				return databaseType;
			}
		}
		throw new IllegalStateException(
				"No database type found for engine:'" + engineName + "'");
	}

}
