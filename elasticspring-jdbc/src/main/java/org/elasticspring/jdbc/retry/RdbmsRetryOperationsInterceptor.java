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

import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 *
 */
public class RdbmsRetryOperationsInterceptor extends RetryOperationsInterceptor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RdbmsRetryOperationsInterceptor.class);

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		Object result;

		if (!isRetryContextOperationActive()) {
			assertNoTransactionActive();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Starting a new Retry Interceptor for " + (invocation != null ? invocation.getMethod() : null));
			}
			result = super.invoke(invocation);
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Participating in existing retry operation");
			}
			result = invocation.proceed();
		}

		return result;
	}

	/**
	 * Return whenever there is already a proxy running inside this thread execution. To avoid multiple retries in the
	 * case
	 * if this bean is called by another bean which already has an RetryOperationsInterceptor.
	 *
	 * @return true if there is a {@link org.springframework.retry.RetryContext} available inside the {@link
	 *         RetrySynchronizationManager} or false otherwise.
	 */
	protected boolean isRetryContextOperationActive() {
		return RetrySynchronizationManager.getContext() != null;
	}

	/**
	 * Check that there is no current transaction active. This should never happen as this interceptor must run actually
	 * before a transaction is created, to ensure a new transaction is started while retrying.
	 */
	private static void assertNoTransactionActive() {
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			throw new JdbcRetryException("An active transaction was found.  This is not allowed when starting a retryable operation.");
		}
	}
}
