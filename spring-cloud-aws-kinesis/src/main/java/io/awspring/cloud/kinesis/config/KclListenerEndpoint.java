/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.kinesis.config;

import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointMode;
import io.awspring.cloud.kinesis.listener.retrieval.KinesisConsumerResolver;
import io.awspring.cloud.kinesis.listener.retrieval.RetrievalMode;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.util.Assert;
import software.amazon.kinesis.common.InitialPositionInStream;

/**
 * {@link KclHandlerMethodEndpoint} implementation describing a single
 * {@link io.awspring.cloud.kinesis.annotation.KclListener} annotated method.
 *
 * @author Matej Nedic
 * @since 4.2.0
 */
public class KclListenerEndpoint implements KclHandlerMethodEndpoint {

	private final String id;

	private final Collection<String> streamNames;

	private final String applicationName;

	@Nullable
	private final String factoryBeanName;

	private final Object bean;

	private final Method method;

	@Nullable
	private final KclCheckpointMode checkpointMode;

	@Nullable
	private final RetrievalMode retrievalMode;

	@Nullable
	private final InitialPositionInStream initialPositionInStream;

	@Nullable
	private final String consumerArn;

	@Nullable
	private final String consumerName;

	@Nullable
	private final String leaseTableName;

	@Nullable
	private final String metricsNamespace;

	@Nullable
	private final String replyStream;

	@Nullable
	private MessageHandlerMethodFactory handlerMethodFactory;

	private KclListenerEndpoint(Builder builder) {
		Assert.hasText(builder.id, "id must not be empty");
		Assert.notEmpty(builder.streamNames, "streamNames must not be empty");
		Assert.hasText(builder.applicationName, "applicationName must not be empty");
		Assert.notNull(builder.bean, "bean must not be null");
		Assert.notNull(builder.method, "method must not be null");
		this.id = builder.id;
		this.streamNames = List.copyOf(builder.streamNames);
		this.applicationName = builder.applicationName;
		this.factoryBeanName = builder.factoryBeanName;
		this.bean = builder.bean;
		this.method = builder.method;
		this.checkpointMode = builder.checkpointMode;
		this.retrievalMode = builder.retrievalMode;
		this.initialPositionInStream = builder.initialPositionInStream;
		this.consumerArn = builder.consumerArn;
		this.consumerName = builder.consumerName;
		this.leaseTableName = builder.leaseTableName;
		this.metricsNamespace = builder.metricsNamespace;
		this.replyStream = builder.replyStream;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public Collection<String> getStreamNames() {
		return this.streamNames;
	}

	@Override
	public String getApplicationName() {
		return this.applicationName;
	}

	@Nullable
	@Override
	public String getFactoryBeanName() {
		return this.factoryBeanName;
	}

	@Override
	public Object getBean() {
		return this.bean;
	}

	@Override
	public Method getMethod() {
		return this.method;
	}

	@Override
	public void setHandlerMethodFactory(MessageHandlerMethodFactory handlerMethodFactory) {
		this.handlerMethodFactory = handlerMethodFactory;
	}

	@Override
	public MessageHandlerMethodFactory getHandlerMethodFactory() {
		Assert.notNull(this.handlerMethodFactory, "handlerMethodFactory has not been set");
		return this.handlerMethodFactory;
	}

	@Nullable
	@Override
	public KclCheckpointMode getCheckpointMode() {
		return this.checkpointMode;
	}

	@Nullable
	@Override
	public RetrievalMode getRetrievalMode() {
		return this.retrievalMode;
	}

	@Nullable
	@Override
	public InitialPositionInStream getInitialPositionInStream() {
		return this.initialPositionInStream;
	}

	@Nullable
	@Override
	public String getConsumerArn() {
		return this.consumerArn;
	}

	@Nullable
	@Override
	public String getConsumerName() {
		return this.consumerName;
	}

	@Nullable
	@Override
	public String getLeaseTableName() {
		return this.leaseTableName;
	}

	@Nullable
	@Override
	public String getMetricsNamespace() {
		return this.metricsNamespace;
	}

	@Nullable
	@Override
	public String getReplyStream() {
		return this.replyStream;
	}

	public static final class Builder {

		@Nullable
		private String id;

		@Nullable
		private Collection<String> streamNames;

		@Nullable
		private String applicationName;

		@Nullable
		private String factoryBeanName;

		@Nullable
		private Object bean;

		@Nullable
		private Method method;

		@Nullable
		private KclCheckpointMode checkpointMode;

		@Nullable
		private RetrievalMode retrievalMode;

		@Nullable
		private InitialPositionInStream initialPositionInStream;

		@Nullable
		private String consumerArn;

		@Nullable
		private String consumerName;

		@Nullable
		private String leaseTableName;

		@Nullable
		private String metricsNamespace;

		@Nullable
		private String replyStream;

		private Builder() {
		}

		public Builder id(String id) {
			this.id = id;
			return this;
		}

		public Builder streamNames(Collection<String> streamNames) {
			this.streamNames = streamNames;
			return this;
		}

		public Builder streamName(String streamName) {
			return streamNames(List.of(streamName));
		}

		public Builder applicationName(String applicationName) {
			this.applicationName = applicationName;
			return this;
		}

		public Builder factoryBeanName(@Nullable String factoryBeanName) {
			this.factoryBeanName = factoryBeanName;
			return this;
		}

		public Builder bean(Object bean) {
			this.bean = bean;
			return this;
		}

		public Builder method(Method method) {
			this.method = method;
			return this;
		}

		public Builder checkpointMode(@Nullable KclCheckpointMode checkpointMode) {
			this.checkpointMode = checkpointMode;
			return this;
		}

		public Builder retrievalMode(@Nullable RetrievalMode retrievalMode) {
			this.retrievalMode = retrievalMode;
			return this;
		}

		public Builder initialPositionInStream(@Nullable InitialPositionInStream initialPositionInStream) {
			this.initialPositionInStream = initialPositionInStream;
			return this;
		}

		public Builder consumerName(@Nullable String consumerNameOrArn) {
			this.consumerArn = consumerNameOrArn != null ? KinesisConsumerResolver.resolveConsumerArn(consumerNameOrArn)
					: null;
			this.consumerName = consumerNameOrArn != null
					? KinesisConsumerResolver.resolveConsumerName(consumerNameOrArn)
					: null;
			return this;
		}

		public Builder leaseTableName(@Nullable String leaseTableName) {
			this.leaseTableName = leaseTableName;
			return this;
		}

		public Builder metricsNamespace(@Nullable String metricsNamespace) {
			this.metricsNamespace = metricsNamespace;
			return this;
		}

		public Builder replyStream(@Nullable String replyStream) {
			this.replyStream = replyStream;
			return this;
		}

		public KclListenerEndpoint build() {
			return new KclListenerEndpoint(this);
		}

	}

}
