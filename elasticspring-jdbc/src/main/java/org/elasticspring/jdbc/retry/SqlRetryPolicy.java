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

package org.elasticspring.jdbc.retry;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.context.RetryContextSupport;

import java.net.ConnectException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class SqlRetryPolicy implements RetryPolicy {

	private static final Logger LOGGER = LoggerFactory.getLogger(SqlRetryPolicy.class);

	private final BinaryExceptionClassifier binaryExceptionClassifier = new BinaryExceptionClassifier(getSqlRetryAbleExceptions(), false);

	private int maxNumberOfRetries = 3;

	private static Map<Class<? extends Throwable>, Boolean> getSqlRetryAbleExceptions() {
		Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<Class<? extends Throwable>, Boolean>();
		retryableExceptions.put(SQLTransientException.class, true);
		retryableExceptions.put(SQLRecoverableException.class, true);
		retryableExceptions.put(TransientDataAccessException.class, true);
		retryableExceptions.put(SQLNonTransientConnectionException.class, true);
		retryableExceptions.put(ConnectException.class, true);
		return retryableExceptions;
	}

	@Override
	public boolean canRetry(RetryContext context) {
		Throwable candidate = context.getLastThrowable();
		if (candidate == null) {
			return true;
		}

		boolean retry = this.binaryExceptionClassifier.classify(candidate);
		if (LOGGER.isTraceEnabled() && !retry) {
			LOGGER.trace("Retry on Exception: {} not possible trying cause", candidate.getClass().getName());
		}

		while (!retry && candidate.getCause() != null) {
			candidate = candidate.getCause();
			retry = this.binaryExceptionClassifier.classify(candidate);
		}

		if (LOGGER.isTraceEnabled() && retry) {
			LOGGER.trace("Retry possible due to exception class {}", candidate.getClass().getName());
		}

		return context.getRetryCount() <= this.maxNumberOfRetries && retry;
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

	public void setMaxNumberOfRetries(int maxNumberOfRetries) {
		this.maxNumberOfRetries = maxNumberOfRetries;
	}
}