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

package org.springframework.cloud.aws.jdbc.datasource;

import javax.sql.DataSource;

/**
 * Factory to create {@link DataSource} instances at runtime. In contrast to the regular
 * datasource definitions, this interface allows the dynamic creation of datasource at
 * runtime. This is especially useful and needed if the datasource information like the
 * url, username and password are not available at configuration time. With this factory
 * it is possible to create datasource with configuration information which is fetched
 * while starting the application. Because of the dynamic creation of datasource at
 * runtime, this interface also provides the lifecycle method
 * {@link #closeDataSource(javax.sql.DataSource)} to actually shutdown the created
 * datasource. This method should be called while destroying the application itself.
 * <p>
 * <b>Note:</b> This interface does not assume what kind of datasource is returned. It is
 * strongly recommended to use an already existing and pooled datasource like
 * <a href="https://tomcat.apache.org/tomcat-7.0-doc/jdbc-pool.html">Tomcat JDBC
 * datasource</a> for real life production applications.
 * </p>
 *
 * @author Agim Emruli
 * @since 1.0
 */
public interface DataSourceFactory {

	/**
	 * Creates a datasource with the passed in information. The datasource itself should
	 * be fully constructed and initialized to be used in a multi threaded environment.
	 * @param dataSourceInformation - the {@link DataSourceInformation} parameter object
	 * which holds all dynamic information for the datasource creation.
	 * @return - a fully initialized datasource instance which can be used by the
	 * application to actually interact with a database platform
	 */
	DataSource createDataSource(DataSourceInformation dataSourceInformation);

	/**
	 * Will be called if the datasource is not used anymore to allow the factory to
	 * release any resource that are used by the created object. Implementation should
	 * check if the passed in datasource is a "known" datasource and if so interact with
	 * the to shutdown the datasource. On well known datasource like Apache Commons DBCP
	 * and Apache Tomcat JDBC this method will call the close() method to release any
	 * connection which are held in the pool.
	 * @param dataSource - The datasource that is not used anymore and can be destroyed by
	 * the factory
	 */
	void closeDataSource(DataSource dataSource);

}
