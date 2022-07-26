/*
 * Copyright 2022 the original author or authors.
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
package io.awspring.cloud.sqs.listener.pipeline;

import io.awspring.cloud.sqs.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.listener.acknowledgement.AckHandler;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import org.springframework.util.Assert;

import java.util.Collection;

/**
 * Holds the components that will be used on the {@link MessageProcessingPipeline}.
 * Every stage will receive it in its constructor, so it can properly configure itself.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class MessageProcessingConfiguration<T> {

	private final Collection<AsyncMessageInterceptor<T>> messageInterceptors;

	private final AsyncMessageListener<T> messageListener;

	private final AsyncErrorHandler<T> errorHandler;

	private final AckHandler<T> ackHandler;

	private MessageProcessingConfiguration(Builder<T> builder) {
		this.messageInterceptors = builder.messageInterceptors;
		this.messageListener = builder.messageListener;
		this.errorHandler = builder.errorHandler;
		this.ackHandler = builder.ackHandler;
	}

	public static <T> MessageProcessingConfiguration.Builder<T> builder() {
		return new Builder<>();
	}

	public Collection<AsyncMessageInterceptor<T>> getMessageInterceptors() {
		return this.messageInterceptors;
	}

	public AsyncMessageListener<T> getMessageListener() {
		return this.messageListener;
	}

	public AsyncErrorHandler<T> getErrorHandler() {
		return this.errorHandler;
	}

	public AckHandler<T> getAckHandler() {
		return this.ackHandler;
	}

	public static class Builder<T> {

		private Collection<AsyncMessageInterceptor<T>> messageInterceptors;
		private AsyncMessageListener<T> messageListener;
		private AsyncErrorHandler<T> errorHandler;
		private AckHandler<T> ackHandler;

		public Builder<T> interceptors(Collection<AsyncMessageInterceptor<T>> messageInterceptors) {
			this.messageInterceptors = messageInterceptors;
			return this;
		}

		public Builder<T> messageListener(AsyncMessageListener<T> messageListener) {
			this.messageListener = messageListener;
			return this;
		}

		public Builder<T> errorHandler(AsyncErrorHandler<T> errorHandler) {
			this.errorHandler = errorHandler;
			return this;
		}

		public Builder<T> ackHandler(AckHandler<T> ackHandler) {
			this.ackHandler = ackHandler;
			return this;
		}

		public MessageProcessingConfiguration<T> build() {
			Assert.notNull(this.messageListener, "messageListener cannot be null provided");
			Assert.notNull(this.errorHandler, "No error handler provided");
			Assert.notNull(this.ackHandler, "No ackHandler provided");
			Assert.notNull(this.messageInterceptors, "messageInterceptors cannot be null");
			return new MessageProcessingConfiguration<>(this);
		}
	}

}
