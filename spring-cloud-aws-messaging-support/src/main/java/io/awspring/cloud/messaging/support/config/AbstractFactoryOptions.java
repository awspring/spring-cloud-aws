/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.messaging.support.config;

import io.awspring.cloud.messaging.support.listener.AsyncErrorHandler;
import io.awspring.cloud.messaging.support.listener.AsyncMessageInterceptor;
import io.awspring.cloud.messaging.support.listener.acknowledgement.AsyncAckHandler;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractFactoryOptions<T, O extends AbstractFactoryOptions<?, ?>> implements FactoryOptions {

	private AsyncErrorHandler<T> errorHandler;

	private AsyncAckHandler<T> ackHandler;

	private AsyncMessageInterceptor<T> messageInterceptor;

	private Integer maxWorksPerContainer;

	Integer getMaxWorkersPerContainer() {
		return this.maxWorksPerContainer;
	}

	AsyncErrorHandler<T> getErrorHandler() {
		return this.errorHandler;
	}

	AsyncAckHandler<T> getAckHandler() {
		return this.ackHandler;
	}

	AsyncMessageInterceptor<T> getMessageInterceptor() {
		return this.messageInterceptor;
	}

	@SuppressWarnings("unchecked")
	private O self() {
		return (O) this;
	}

	public O errorHandler(AsyncErrorHandler<T> errorHandler) {
		this.errorHandler = errorHandler;
		return self();
	}

	public O ackHandler(AsyncAckHandler<T> ackHandler) {
		this.ackHandler = ackHandler;
		return self();
	}

	public O interceptor(AsyncMessageInterceptor<T> messageInterceptor) {
		this.messageInterceptor = messageInterceptor;
		return self();
	}

	public O concurrentWorkersPerContainer(int maxWorksPerContainer) {
		this.maxWorksPerContainer = maxWorksPerContainer;
		return self();
	}
}
