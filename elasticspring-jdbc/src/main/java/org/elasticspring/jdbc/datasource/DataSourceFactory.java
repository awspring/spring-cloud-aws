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

import javax.sql.DataSource;

/**
 * Factory to create {@link DataSource} instances at runtime. In contrast to the regular data source definitions, this
 * interface allows the dynamic creation of data source at runtime. This is especially useful and needed if the data
 * source information like the url, username and password are not available at configuration time. With this factory it
 * is possible to create data source with configuration information which are fetched while starting the application.
 * Because the dynamic creation of data source at runtime this interface also provides the lifecycle method {@link
 * #closeDataSource(javax.sql.DataSource)} to actually shutdown the created data source. This method should be called
 * while destroying the application itself.
 * <p/>
 * <b>Note:</b> This interface does not assume what kind of data source is returned. It is strongly recommended to use
 * an already existing and pooled data source like <a href="http://tomcat.apache.org/tomcat-7.0-doc/jdbc-pool.html">Tomcat
 * JDBC data source</a> for real life production applications.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public interface DataSourceFactory {

	/**
	 * Crates a data source with the passed in information. The data source itself should be fully constructed and
	 * initialized to be used in a multi threaded environment.
	 *
	 * @param dataSourceInformation
	 * 		- the {@link DataSourceInformation} parameter object which holds all dynamic information for the data source
	 * 		creation.
	 * @return - a fully initialized data source instance which can be used by the application to actually interact with a
	 *         database platform
	 */
	DataSource createDataSource(DataSourceInformation dataSourceInformation);

	/**
	 * Will be called if the data source is not used anymore to allow the factory to release any resource that are used by
	 * the created object. Implementation should check if the passed in data source is a "known" data source and if so
	 * interact with the to shut down the data source. On well known data source like Apache Commons DBCP and Apache
	 * Tomcat
	 * JDBC this method will call the close() method to release any connection which are held in the pool.
	 *
	 * @param dataSource
	 * 		- The data source that is not used anymore and can be destroyed by the factory
	 */
	void closeDataSource(DataSource dataSource);

}