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

import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Subclass of {@link RetryOperationsInterceptor} that checks that there is no transaction
 * available while starting a retryable operation. This class also ensures that there is
 * only one outer retry operation in case of nested retryable methods.
 * <p>
 * This allows service to call other service that might have a retry interceptor
 * configured.
 * </p>
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class RdbmsRetryOperationsInterceptor extends RetryOperationsInterceptor {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(RdbmsRetryOperationsInterceptor.class);

	/**
	 * Checks that there is no current transaction active. This should never happen as
	 * this interceptor must run actually before a transaction is created, to ensure a new
	 * transaction is started while retrying.
	 */
	private static void assertNoTransactionActive() {
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			throw new JdbcRetryException(
					"An active transaction was found.  This is not allowed when starting a retryable operation.");
		}
	}

	/**
	 * Checks that there is no retry operation open before delegating to the method
	 * {@link RetryOperationsInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)}
	 * method. Execute the MethodInvocation directly if there is already a
	 * {@link org.springframework.retry.RetryContext} available for the current thread
	 * execution.
	 * @param invocation - the method invocation that is the target of this interceptor
	 * @return the result of the method invocation
	 * @throws Throwable - the exception thrown by the method invocations target or a
	 * {@link JdbcRetryException} if there is already a transaction available.
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		Object result;

		if (!isRetryContextOperationActive()) {
			assertNoTransactionActive();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Starting a new Retry Interceptor for {}",
						(invocation != null ? invocation.getMethod() : null));
			}
			result = super.invoke(invocation);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Finished a new Retry Interceptor for {}",
						(invocation != null ? invocation.getMethod() : null));
			}
		}
		else {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Participating in existing retry operation");
			}
			result = invocation.proceed();
		}

		return result;
	}

	/**
	 * Returns whenever there is already a proxy running inside this thread execution. To
	 * avoid multiple retries in the case if this bean is called by another bean which
	 * already has a RetryOperationsInterceptor.
	 * @return <code>true</code> if there is a
	 * {@link org.springframework.retry.RetryContext} available inside the
	 * {@link RetrySynchronizationManager} or <code>false</code> otherwise.
	 */
	protected boolean isRetryContextOperationActive() {
		return RetrySynchronizationManager.getContext() != null;
	}

}
