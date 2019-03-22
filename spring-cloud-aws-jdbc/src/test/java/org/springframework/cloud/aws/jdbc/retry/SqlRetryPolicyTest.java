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

package org.springframework.cloud.aws.jdbc.retry;

import java.sql.SQLException;
import java.sql.SQLTransientException;

import org.junit.Test;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.retry.context.RetryContextSupport;
import org.springframework.transaction.TransactionSystemException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test class for the {@link SqlRetryPolicy}.
 *
 * @author Agim Emruli
 */
public class SqlRetryPolicyTest {

	@Test
	public void testRetryTransientExceptions() throws Exception {
		SqlRetryPolicy sqlRetryPolicy = new SqlRetryPolicy();
		RetryContextSupport retryContext = new RetryContextSupport(null);

		retryContext.registerThrowable(new SQLTransientException("foo"));
		assertThat(sqlRetryPolicy.canRetry(retryContext)).isTrue();

		retryContext.registerThrowable(new TransientDataAccessResourceException("foo"));
		assertThat(sqlRetryPolicy.canRetry(retryContext)).isTrue();
	}

	@Test
	public void testNoRetryPersistentExceptions() throws Exception {
		SqlRetryPolicy sqlRetryPolicy = new SqlRetryPolicy();
		RetryContextSupport retryContext = new RetryContextSupport(null);

		retryContext.registerThrowable(new SQLException("foo"));
		assertThat(sqlRetryPolicy.canRetry(retryContext)).isFalse();

		retryContext.registerThrowable(new DataAccessResourceFailureException("foo"));
		assertThat(sqlRetryPolicy.canRetry(retryContext)).isFalse();
	}

	@Test
	public void testWithNestedException() throws Exception {
		SqlRetryPolicy sqlRetryPolicy = new SqlRetryPolicy();
		RetryContextSupport retryContext = new RetryContextSupport(null);

		retryContext.registerThrowable(new TransactionSystemException(
				"Could not commit JDBC transaction", new SQLTransientException("foo")));
		assertThat(sqlRetryPolicy.canRetry(retryContext)).isTrue();
	}

	@Test
	public void testMaxRetriesReached() throws Exception {
		SqlRetryPolicy sqlRetryPolicy = new SqlRetryPolicy();
		sqlRetryPolicy.setMaxNumberOfRetries(3);
		RetryContextSupport retryContext = new RetryContextSupport(null);
		retryContext.registerThrowable(new SQLTransientException("foo"));
		assertThat(sqlRetryPolicy.canRetry(retryContext)).isTrue();
		retryContext.registerThrowable(new SQLTransientException("foo"));
		assertThat(sqlRetryPolicy.canRetry(retryContext)).isTrue();
		retryContext.registerThrowable(new SQLTransientException("foo"));
		assertThat(sqlRetryPolicy.canRetry(retryContext)).isTrue();
		retryContext.registerThrowable(new SQLTransientException("foo"));
		assertThat(sqlRetryPolicy.canRetry(retryContext)).isFalse();
	}

}
