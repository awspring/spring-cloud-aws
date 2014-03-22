/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.jdbc.datasource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Dynamic datasource with regards to the configuration and initialization. This datasource is useful if the
 * configuration is not available at configuration time and if the datasource initialization is time consuming.
 * <p><b>Configuration: </b>This datasource retrieves all information like host name, port and user through through
 * the {@link DataSourceInformation} object and uses the {@link DataSourceFactory} class to actually instantiate the
 * class. The passed in DataSourceFactory will typically create a datasource connection pool through an
 * well-known connection pool like Apache Commons DBCP or Tomcat JDBC.
 * <p><b>Initialization:</b> This class uses a {@link DataSourceStatus} strategy before actually trying to initialize
 * the
 * datasource. This is especially useful if the datasource is in a "non-available" state because it is bootstrapped
 * at the same time like the application. To avoid long application bootstrap times it is recommended to use an
 * asynchronous {@link TaskExecutor} to allow the datasource creation to run in the background. In that case the
 * {@link #getConnection()} calls will wait for the datasource to become available. The initialization is initiated
 * through the {@link #afterPropertiesSet()} method.</p>
 *
 * @author Agim Emruli
 * @since 1.0
 */
public final class DynamicDataSource extends AbstractDataSource implements InitializingBean {

	private static final int TIMEOUT = 5000;
	private final Object dataSourceMonitor = new Object();
	private final DataSourceInformation dataSourceInformation;
	private final DataSourceFactory dataSourceFactory;
	private final DataSourceStatus dataSourceStatus;
	private final TaskExecutor taskExecutor;
	/**
	 * Flag the indicated if this datasource is still active or has been requested to shutdown while bootstrapping.
	 * This is typically the case if the application bootstrap process is cancelled due to some errors in the
	 * configuration or initialization of the application itself.
	 */
	private volatile boolean active;
	/**
	 * Internal target datasource that will provide the actual connection once it is initialized
	 */
	private volatile DataSource dataSource;

	/**
	 * Constructor that receives all strategies needed to create the underlying datasource. Note that all values are
	 * immutable and therefore can not be changed at runtime. This constructor also initializes the datasource. In
	 * case of an asynchronous {@link TaskExecutor} like {@link org.springframework.core.task.SimpleAsyncTaskExecutor}
	 * this constructor kicks-off the construction process. If you want to have a synchronous creation of the data
	 * source then pass in an {@link org.springframework.core.task.SyncTaskExecutor}.
	 *
	 * @param dataSourceFactory
	 * 		The datasource factory implementation that will be used to instantiate the underlying
	 * 		datasource. The datasource may contain implementation specific configuration
	 * 		information like the pool size.
	 * @param dataSourceInformation
	 * 		Contains all the information that are dynamic and typically retrieved at runtime. Like the host name of the
	 * 		physical database server.
	 * @param dataSourceStatus
	 * 		Strategy interface which provides information if the datasource itself is available or still bootstrapping.
	 * @param taskExecutor
	 * 		Implementation of a Spring {@link TaskExecutor} which is used to query the {@link DataSourceStatus} and construct
	 * 		the datasource afterwards. Based on the implementation this can be done in a synchronous or asynchronous way.
	 */
	public DynamicDataSource(DataSourceInformation dataSourceInformation, DataSourceFactory dataSourceFactory,
							 DataSourceStatus dataSourceStatus, TaskExecutor taskExecutor) {
		this.dataSourceStatus = dataSourceStatus;
		this.dataSourceInformation = dataSourceInformation;
		this.dataSourceFactory = dataSourceFactory;
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Provides JDBC connection for the calling code in order to interact with the JDBC database system. Check if this
	 * datasource is available and already initialized. Both checks are not expensive once the underlying datasource has
	 * been initialized.
	 * <p>All the calls to this method blocks until the datasource is initialized. In normal situations, the datasource
	 * will already be available on the first call of this method. However, the calling code may have to wait until the
	 * datasource is initialized or this class is shutdown through the method {@link #destroyDataSource()}.</p>
	 * <p><b>Note:</b>If this instance is shut down while doing a method call, this method will throw an
	 * IllegalStateException</p>
	 *
	 * @return The connection which is provided by the underlying datasource. Typically a pooled connection.
	 * @throws SQLException
	 * 		The exception that is thrown by the underlying datasource implementation.
	 * @throws IllegalArgumentException
	 * 		If this datasource is not active because it has been shutdown while calling this method.
	 */
	@Override
	public Connection getConnection() throws SQLException {
		assertDataSourceAvailable();
		return this.dataSource.getConnection();
	}

	/**
	 * Provides the same semantics like {@link #getConnection()} except that it allow to use a custom username and
	 * password on each method call instead of a fixed one which is provided by the datasource.
	 *
	 * @param username
	 * 		The username that will be used to establish the connection
	 * @param password
	 * 		The password that will be used to establish the connection
	 * @return The connection which is provided by the underlying datasource. Typically a pooled connection.
	 * @throws SQLException
	 * 		The exception that is thrown by the underlying datasource implementation.
	 * @throws IllegalArgumentException
	 * 		If this datasource is not active because it has been shutdown while calling this method.
	 */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		assertDataSourceAvailable();
		return this.dataSource.getConnection(username, password);
	}

	/**
	 * Destroys the datasource and propagates this call to the underlying DataSourceFactory in order to close any open
	 * connections held in a pool. Calling this method also interrupts the initialization process, this might be
	 * especially
	 * the case if the application shutdowns while initializing the datasource.
	 */
	public void destroyDataSource() {
		synchronized (this.dataSourceMonitor) {
			this.active = false;
			this.dataSourceFactory.closeDataSource(this.dataSource);
			this.dataSourceMonitor.notifyAll();
		}
	}

	private void assertDataSourceAvailable() {
		assertActive();
		if (this.dataSource == null) {
			try {
				synchronized (this.dataSourceMonitor) {
					while (this.dataSource == null) {
						assertActive();
						this.dataSourceMonitor.wait(TIMEOUT);
					}
				}
			} catch (InterruptedException e) {
				// Re-interrupt current thread, to allow other threads to react.
				Thread.currentThread().interrupt();
				throw new IllegalStateException("DynamicDataSource has been already closed");
			}
		}
	}

	private void assertActive() {
		if (!this.active) {
			throw new IllegalStateException("DynamicDataSource has been already closed or not initialized");
		}
	}

	private void initializeDataSource() {

		this.taskExecutor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					synchronized (DynamicDataSource.this.dataSourceMonitor) {
						while (DynamicDataSource.this.active && DynamicDataSource.this.dataSource == null &&
								!DynamicDataSource.this.dataSourceStatus.isDataSourceAvailable()) {
							DynamicDataSource.this.dataSourceMonitor.wait(TIMEOUT);
						}
						DynamicDataSource.this.dataSource = DynamicDataSource.this.dataSourceFactory.createDataSource(DynamicDataSource.this.dataSourceInformation);
						DynamicDataSource.this.dataSourceMonitor.notifyAll();
					}
				} catch (InterruptedException e) {
					// Re-interrupt current thread, to allow other threads to react.
					Thread.currentThread().interrupt();
				}
			}
		});
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		synchronized (this.dataSourceMonitor) {
			this.active = true;
			initializeDataSource();
		}
	}

	/**
	 * SPI interface that will be used by the DynamicDataSource to recognize if a datasource is available or not. This is
	 * the barrier before actually creating the datasource. Implementation could ask the cloud service if the database
	 * service is available and return the result of the computation.
	 */
	public interface DataSourceStatus {

		/**
		 * Returns whenever the datasource is available or not. This method will be called periodically until it returns
		 * true
		 * so that the datasource is created.
		 *
		 * @return true if the datasource is available. False while the datasource is not available.
		 */
		boolean isDataSourceAvailable();
	}
}