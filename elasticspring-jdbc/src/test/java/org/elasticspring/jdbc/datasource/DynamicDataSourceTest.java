/*
 * Copyright 2013-2014 the original author or authors.
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

import org.elasticspring.jdbc.datasource.support.DatabaseType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test class for {@see DynamicDataSource}
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class DynamicDataSourceTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Rule
	public Timeout timeout = new Timeout(30 * 1000);

	@Test
	public void testGetConnectionWithReadyDataSource() throws Exception {
		DataSourceInformation dataSourceInformation = new DataSourceInformation(DatabaseType.MYSQL, "localhost", 3306, "testDb", "admin", "secret");
		DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		when(dataSourceFactory.createDataSource(dataSourceInformation)).thenReturn(dataSource);
		when(dataSource.getConnection()).thenReturn(connection);

		DynamicDataSource dynamicDataSource = new DynamicDataSource(dataSourceInformation, dataSourceFactory, new SimpleDataSourceStatus(true), new SimpleAsyncTaskExecutor());
		dynamicDataSource.afterPropertiesSet();

		Connection borrowedConnection = dynamicDataSource.getConnection();
		assertNotNull(borrowedConnection);
	}

	@Test
	public void testGetConnectionWithReadyUsernameAndPassword() throws Exception {
		DataSourceInformation dataSourceInformation = new DataSourceInformation(DatabaseType.MYSQL, "localhost", 3306, "testDb", "admin", "secret");
		DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		when(dataSourceFactory.createDataSource(dataSourceInformation)).thenReturn(dataSource);
		when(dataSource.getConnection("user", "password")).thenReturn(connection);

		DynamicDataSource dynamicDataSource = new DynamicDataSource(dataSourceInformation, dataSourceFactory, new SimpleDataSourceStatus(true), new SimpleAsyncTaskExecutor());
		dynamicDataSource.afterPropertiesSet();

		Connection borrowedConnection = dynamicDataSource.getConnection("user", "password");
		assertNotNull(borrowedConnection);
	}

	@Test
	public void testGetConnectionNonReadyDataSourceConcurrent() throws Exception {
		DataSourceInformation dataSourceInformation = new DataSourceInformation(DatabaseType.MYSQL, "localhost", 3306, "testDb", "admin", "secret");
		DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		when(dataSourceFactory.createDataSource(dataSourceInformation)).thenReturn(dataSource);
		when(dataSource.getConnection()).thenReturn(connection);

		final CountDownLatch countDownLatch = new CountDownLatch(20);

		SimpleAsyncTaskExecutor taskScheduler = new SimpleAsyncTaskExecutor();
		SimpleDataSourceStatus dataSourceStatus = new SimpleDataSourceStatus(false);
		final DynamicDataSource dynamicDataSource = new DynamicDataSource(dataSourceInformation, dataSourceFactory, dataSourceStatus, taskScheduler);
		dynamicDataSource.afterPropertiesSet();

		for (int i = 0; i < 20; i++) {
			taskScheduler.execute(new Runnable() {

				@Override
				public void run() {
					try {
						Connection result = dynamicDataSource.getConnection();
						countDownLatch.countDown();
						assertNotNull(result);
					} catch (SQLException e) {
						fail(e.getMessage());
					}
				}
			});
		}
		dataSourceStatus.setStatus(true);

		//Await all threads to really get the connection
		countDownLatch.await();
	}


	@Test
	public void testGetConnectionTerminatesWhileShuttingDown() throws Exception {
		DataSourceInformation dataSourceInformation = new DataSourceInformation(DatabaseType.MYSQL, "localhost", 3306, "testDb", "admin", "secret");
		DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		when(dataSourceFactory.createDataSource(dataSourceInformation)).thenReturn(dataSource);
		when(dataSource.getConnection()).thenReturn(connection);

		final CountDownLatch countDownLatch = new CountDownLatch(5);

		ThreadPoolTaskExecutor taskScheduler = new ThreadPoolTaskExecutor();
		taskScheduler.setQueueCapacity(0);
		taskScheduler.afterPropertiesSet();

		SimpleDataSourceStatus dataSourceStatus = new SimpleDataSourceStatus(false);
		final DynamicDataSource dynamicDataSource = new DynamicDataSource(dataSourceInformation, dataSourceFactory, dataSourceStatus, taskScheduler);
		dynamicDataSource.afterPropertiesSet();

		for (int i = 0; i < 5; i++) {
			taskScheduler.execute(new Runnable() {

				@Override
				public void run() {
					try {
						countDownLatch.countDown();
						dynamicDataSource.getConnection();
						fail("Datasource has been already closed, hence to connection should be returned");
					} catch (IllegalStateException ise) {
						assertTrue(ise.getMessage().contains("closed"));
					} catch (SQLException e) {
						fail(e.getMessage());
					}
				}
			});
		}

		//Await all threads to reach a getConnection call
		countDownLatch.await();
		taskScheduler.shutdown();
	}

	@Test
	public void testDynamicDataSourceIsAlreadyClosed() throws Exception {
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("closed");

		DataSourceInformation dataSourceInformation = new DataSourceInformation(DatabaseType.MYSQL, "localhost", 3306, "testDb", "admin", "secret");
		DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);

		DynamicDataSource dynamicDataSource = new DynamicDataSource(dataSourceInformation, dataSourceFactory, new SimpleDataSourceStatus(true), new SimpleAsyncTaskExecutor());
		dynamicDataSource.afterPropertiesSet();

		dynamicDataSource.destroyDataSource();
		dynamicDataSource.getConnection();
	}

	@Test
	public void testDynamicDataSourceIsNotInitializedClosed() throws Exception {
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("closed");

		DataSourceInformation dataSourceInformation = new DataSourceInformation(DatabaseType.MYSQL, "localhost", 3306, "testDb", "admin", "secret");
		DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);

		DynamicDataSource dynamicDataSource = new DynamicDataSource(dataSourceInformation, dataSourceFactory, new SimpleDataSourceStatus(true), new SimpleAsyncTaskExecutor());
		dynamicDataSource.getConnection();
	}


	@Test
	public void testDynamicDataSourceDestroyedWhileInitializing() throws Exception {
		DataSourceInformation dataSourceInformation = new DataSourceInformation(DatabaseType.MYSQL, "localhost", 3306, "testDb", "admin", "secret");
		DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);

		final CountDownLatch taskCountDownLatch = new CountDownLatch(1);

		ThreadPoolTaskExecutor taskScheduler = new ThreadPoolTaskExecutor() {

			@Override
			public void execute(final Runnable task) {
				super.execute(new Runnable() {

					@Override
					public void run() {
						task.run();
						taskCountDownLatch.countDown();
					}
				});
			}
		};
		taskScheduler.setCorePoolSize(0);
		taskScheduler.afterPropertiesSet();

		final CountDownLatch countDownLatch = new CountDownLatch(1);

		SimpleDataSourceStatus dataSourceStatus = new SimpleDataSourceStatus(false) {

			@Override
			public boolean isDataSourceAvailable() {
				countDownLatch.countDown();
				return super.isDataSourceAvailable();
			}
		};

		DynamicDataSource dynamicDataSource = new DynamicDataSource(dataSourceInformation, dataSourceFactory, dataSourceStatus, taskScheduler);
		dynamicDataSource.afterPropertiesSet();

		//Make sure that the thread as actually ramped up
		countDownLatch.await(1, TimeUnit.SECONDS);

		//Destroy datasource
		dynamicDataSource.destroyDataSource();

		//Await background thread to be shut down
		taskCountDownLatch.await(5, TimeUnit.SECONDS);

		//destroy scheduler
		taskScheduler.destroy();
	}

	private static class SimpleDataSourceStatus implements DynamicDataSource.DataSourceStatus {

		private volatile boolean status;

		private SimpleDataSourceStatus(boolean status) {
			this.status = status;
		}

		@Override
		public boolean isDataSourceAvailable() {
			return this.status;
		}

		public void setStatus(boolean status) {
			this.status = status;
		}
	}

}
