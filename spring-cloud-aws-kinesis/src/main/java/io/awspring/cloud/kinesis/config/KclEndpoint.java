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

import io.awspring.cloud.kinesis.listener.KclContainerOptions;
import io.awspring.cloud.kinesis.listener.MessageListenerContainer;
import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointMode;
import io.awspring.cloud.kinesis.listener.retrieval.RetrievalMode;
import java.util.Collection;
import org.jspecify.annotations.Nullable;
import software.amazon.kinesis.common.InitialPositionInStream;

/**
 * Represents a Kinesis stream from which records can be consumed by a {@link MessageListenerContainer}.
 * <p>
 * Implementations describe <em>what</em> to consume and which per-listener overrides to apply, while a
 * {@link MessageListenerContainerFactory} decides <em>how</em> the container is created. Attributes returning
 * {@code null} leave the corresponding {@link KclContainerOptions} value configured on the factory untouched.
 *
 * @author Matej Nedic
 * @since 4.2.0
 */
public interface KclEndpoint {

	/**
	 * The id of the container that will handle this endpoint.
	 * @return the container id.
	 */
	String getId();

	/**
	 * The Kinesis streams to consume from, never empty. Several streams are consumed by one container under a single
	 * KCL application.
	 * @return the stream names.
	 */
	Collection<String> getStreamNames();

	/**
	 * The KCL application name used for lease coordination.
	 * @return the application name.
	 */
	String getApplicationName();

	/**
	 * The name of the factory bean that will process this endpoint.
	 * @return the factory bean name, or {@code null} to use the default factory.
	 */
	@Nullable
	String getFactoryBeanName();

	/**
	 * The checkpoint mode overriding the one configured on the factory.
	 * @return the checkpoint mode, or {@code null} to keep the factory value.
	 */
	@Nullable
	KclCheckpointMode getCheckpointMode();

	/**
	 * The retrieval mode overriding the one configured on the factory.
	 * @return the retrieval mode, or {@code null} to keep the factory value.
	 */
	@Nullable
	RetrievalMode getRetrievalMode();

	/**
	 * The initial position overriding the one configured on the factory.
	 * @return the initial position, or {@code null} to keep the factory value.
	 */
	@Nullable
	InitialPositionInStream getInitialPositionInStream();

	/**
	 * The ARN of the enhanced fan-out consumer to read this stream with.
	 * @return the consumer ARN, or {@code null} if none or a name is used instead.
	 */
	@Nullable
	String getConsumerArn();

	/**
	 * The name of the enhanced fan-out consumer to read this stream with.
	 * @return the consumer name, or {@code null} if none or an ARN is used instead.
	 */
	@Nullable
	String getConsumerName();

	/**
	 * The DynamoDB lease table name for this endpoint.
	 * @return the lease table name, or {@code null} to keep the factory value.
	 */
	@Nullable
	String getLeaseTableName();

	/**
	 * The CloudWatch namespace for the KCL metrics of this endpoint.
	 * @return the metrics namespace, or {@code null} to keep the factory value.
	 */
	@Nullable
	String getMetricsNamespace();

	/**
	 * The stream that return values of this endpoint are forwarded to.
	 * @return the reply stream, or {@code null} if return values are not forwarded.
	 */
	@Nullable
	String getReplyStream();

}
