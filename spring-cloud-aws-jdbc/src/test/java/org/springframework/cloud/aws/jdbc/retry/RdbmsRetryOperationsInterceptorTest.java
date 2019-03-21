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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryException;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test class for {@link RdbmsRetryOperationsInterceptor}.
 *
 * @author Agim Emruli
 */
public class RdbmsRetryOperationsInterceptorTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testRetryContextIsAvailable() throws Throwable {
		RetryContext retryContext = mock(RetryContext.class);
		RetrySynchronizationManager.register(retryContext);

		MethodInvocation methodInvocation = mock(MethodInvocation.class);

		try {
			RdbmsRetryOperationsInterceptor interceptor = new RdbmsRetryOperationsInterceptor();
			interceptor.invoke(methodInvocation);
		}
		finally {
			RetrySynchronizationManager.clear();
		}

		verify(methodInvocation, times(1)).proceed();
	}

	@Test
	public void testRetryContextIsNotAvailable() throws Throwable {

		ProxyMethodInvocation methodInvocation = mock(ProxyMethodInvocation.class);

		when(methodInvocation.invocableClone()).thenReturn(methodInvocation);
		when(methodInvocation.proceed()).then(invocation -> {
			assertThat(RetrySynchronizationManager.getContext()).isNotNull();
			return "foo";
		});

		RdbmsRetryOperationsInterceptor interceptor = new RdbmsRetryOperationsInterceptor();
		interceptor.setLabel("mylabel"); // Avoids NPE in
											// RetryOperationsInterceptor.invoke
		interceptor.invoke(methodInvocation);

		verify(methodInvocation, times(1)).invocableClone();
	}

	@Test
	public void testRetryContextWithoutTransaction() throws Throwable {
		this.expectedException.expect(RetryException.class);
		this.expectedException.expectMessage("An active transaction was found");

		TransactionSynchronizationManager.setActualTransactionActive(true);
		try {
			RdbmsRetryOperationsInterceptor operationsInterceptor = new RdbmsRetryOperationsInterceptor();
			MethodInvocation methodInvocation = mock(MethodInvocation.class);
			operationsInterceptor.invoke(methodInvocation);
		}
		finally {
			TransactionSynchronizationManager.setActualTransactionActive(false);
		}
	}

}
