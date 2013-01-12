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

import org.elasticspring.jdbc.datasource.DataSourceInformation;

/**
 * Support interface used by the {@link org.elasticspring.jdbc.datasource.DataSourceFactory} implementation to retrieve
 * the necessary configuration data for every data base platform. As data bases platform have differences in the URL
 * scheme, driver class name this interface provides and abstraction to retrieve the information based on the
 * particular data base platform.
 * <p>Normally this information are provided at configuration time by the user, because of the fact that ElasticSpring
 * creates the data source at runtime, this information must be provided through an registry.</p>
 *
 * @author Agim Emruli
 * @since 1.0
 */
public interface DatabasePlatformSupport {

	/**
	 * Return the fully qualified driver class name for the data base platform. Implementation may return one static
	 * driver
	 * class name or check for the existence of different driver class on the class path and return the best matching one.
	 *
	 * @param databaseType
	 * 		- The database type for which the data base driver class should be returned.
	 * @return The fully qualified driver class name used to connect to the data base.
	 */
	String getDriverClassNameForDatabase(DataSourceInformation.DatabaseType databaseType);

	/**
	 * Construct the data base URL for the data base instance. Implementation will typically infer the URI scheme from the
	 * database type (e.g. for mysql jdbc:mysql). The remaining parts like the hostname and port are used to fully
	 * construct the url based on the data base platform.
	 *
	 * @param databaseType
	 * 		- The databaseType for which the URL should be constructed.
	 * @param hostname
	 * 		- The hostname without any port information used to connect to.
	 * @param port
	 * 		- The port used to connect to the data base
	 * @param databaseName
	 * 		- The data base name used to connect to. The usage is implementation specific (e.g. for Oracle this is the SID)
	 * @return - A fully constructed and valid url for the data source to connect to the data base plattform.
	 */
	String getDatabaseUrlForDatabase(DataSourceInformation.DatabaseType databaseType, String hostname, int port, String databaseName);

}
