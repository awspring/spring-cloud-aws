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

import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.context.RetryContextSupport;
import org.springframework.retry.policy.SimpleRetryPolicy;

/**
 * {@link RetryPolicy} implementation that checks for database error which are retryable.
 * Normally this are well known exceptions inside the JDBC (1.6) exception hierarchy and
 * also the Spring {@link org.springframework.dao.DataAccessException} hierarchy. In
 * addition to that, this class also tries for permanent exception which are related to a
 * connection of the database. This is useful because Amazon RDS database instances might
 * be retryable even if there is a permanent error. This is typically the case in a master
 * a/z failover where the source instance might not be available but a second attempt
 * might succeed because the DNS record has been updated to the failover instance.
 * <p>
 * In contrast to a {@link SimpleRetryPolicy} this class also checks recursively the cause
 * of the exception if there is a retryable implementation.
 * </p>
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class SqlRetryPolicy implements RetryPolicy {

	private static final Logger LOGGER = LoggerFactory.getLogger(SqlRetryPolicy.class);

	/**
	 * BinaryExceptionClassifier used to classify exceptions.
	 */
	private final BinaryExceptionClassifier binaryExceptionClassifier = new BinaryExceptionClassifier(
			getSqlRetryAbleExceptions(), false);

	/**
	 * Holds the maximum number of retries that should be tried if an exception is
	 * retryable.
	 */
	private int maxNumberOfRetries = 3;

	/**
	 * Returns all the exceptions for which a retry is useful.
	 * @return - Map containing all retryable exceptions for the
	 * {@link BinaryExceptionClassifier}
	 */
	private static Map<Class<? extends Throwable>, Boolean> getSqlRetryAbleExceptions() {
		Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
		retryableExceptions.put(SQLTransientException.class, true);
		retryableExceptions.put(SQLRecoverableException.class, true);
		retryableExceptions.put(TransientDataAccessException.class, true);
		retryableExceptions.put(SQLNonTransientConnectionException.class, true);
		return retryableExceptions;
	}

	/**
	 * Returns if this method is retryable based on the {@link RetryContext}. If there is
	 * no Throwable registered, then this method returns <code>true</code> without
	 * checking any further conditions. If there is a Throwable registered, this class
	 * checks if the registered Throwable is a retryable Exception in the context of SQL
	 * exception. If not successful, this class also checks the cause if there is a nested
	 * retryable exception available.
	 * <p>
	 * Before checking exception this class checks that the current retry count (fetched
	 * through {@link org.springframework.retry.RetryContext#getRetryCount()} is smaller
	 * or equals to the {@link #maxNumberOfRetries}
	 * </p>
	 * @param context - the retry context holding information about the retryable
	 * operation (number of retries, throwable if any)
	 * @return <code>true</code> if there is no throwable registered, if there is a
	 * retryable exception and the number of maximum numbers of retries have not been
	 * reached.
	 */
	@Override
	public boolean canRetry(RetryContext context) {
		Throwable candidate = context.getLastThrowable();
		if (candidate == null) {
			return true;
		}
		return context.getRetryCount() <= this.maxNumberOfRetries
				&& isRetryAbleException(candidate);
	}

	@Override
	public RetryContext open(RetryContext parent) {
		return new RetryContextSupport(parent);
	}

	@Override
	public void close(RetryContext context) {

	}

	@Override
	public void registerThrowable(RetryContext context, Throwable throwable) {
		((RetryContextSupport) context).registerThrowable(throwable);
	}

	private boolean isRetryAbleException(Throwable throwable) {
		boolean retryAble = this.binaryExceptionClassifier.classify(throwable);
		if (!retryAble) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Retry on Exception: {} not possible trying cause",
						throwable.getClass().getName());
			}
			if (throwable.getCause() != null) {
				return isRetryAbleException(throwable.getCause());
			}
			return false;
		}
		else {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Retry possible due to exception class {}",
						throwable.getClass().getName());
			}
			return true;
		}
	}

	/**
	 * Configures the maximum number of retries. This number should be a trade-off between
	 * having enough retries to survive a database outage due to failure and a responsive
	 * and not stalling application. The default value for the maximum number is 3.
	 * <p>
	 * <b>Note:</b>Consider using a {@link BackOffPolicy} which ensures that there is
	 * enough time left between the retry attempts instead of increasing this value to a
	 * high number. The back-off policy ensures that there is a delay in between the retry
	 * operations.
	 * </p>
	 * @param maxNumberOfRetries - the maximum number of retries should be a positive
	 * number, otherwise all retries will fail.
	 */
	public void setMaxNumberOfRetries(int maxNumberOfRetries) {
		this.maxNumberOfRetries = maxNumberOfRetries;
	}

}
