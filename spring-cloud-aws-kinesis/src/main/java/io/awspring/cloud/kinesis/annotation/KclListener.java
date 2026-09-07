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
package io.awspring.cloud.kinesis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.messaging.handler.annotation.MessageMapping;

/**
 * Annotation that marks a method to be the target of a Kinesis message listener backed by the Kinesis Client Library
 * (KCL). Each annotated method is handled by a dedicated
 * {@link io.awspring.cloud.kinesis.listener.MessageListenerContainer}, created by the specified {@link #factory()}. If
 * not specified, a default factory is looked up in the context.
 * <p>
 * Properties in this annotation support property placeholders ({@code "${...}"}) and SpEL ({@code "#{...}"}).
 *
 * @author Matej Nedic
 * @since 4.2.0
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@MessageMapping
public @interface KclListener {

	/**
	 * The names of the Kinesis streams to consume from. Alias for {@link #streamNames()}.
	 * @return the stream names.
	 */
	@AliasFor("streamNames")
	String[] value() default {};

	/**
	 * An id for the {@link io.awspring.cloud.kinesis.listener.MessageListenerContainer} that will be created to handle
	 * this endpoint. If none is provided a default id will be generated.
	 * @return the container id.
	 */
	String id() default "";

	/**
	 * The names of the Kinesis streams to consume from. Alias for {@link #value()}.
	 * <p>
	 * Declaring several streams makes one container consume all of them under a single KCL application, so they share
	 * one lease table and one set of workers instead of one application per stream. Group streams this way only when
	 * their records share the payload type the listener method declares, since all of them are dispatched to that one
	 * signature; streams with differing shapes fail conversion at runtime. Multi-stream listeners also share the
	 * checkpoint mode, initial position and metrics namespace, and require {@code POLLING} retrieval, since an enhanced
	 * fan-out consumer belongs to a single stream.
	 * @return the stream names.
	 */
	@AliasFor("value")
	String[] streamNames() default {};

	/**
	 * The KCL application name used for lease coordination. Defaults to the container id when not set.
	 * @return the application name.
	 */
	String applicationName() default "";

	/**
	 * The {@link io.awspring.cloud.kinesis.config.MessageListenerContainerFactory} bean name to be used to process this
	 * endpoint.
	 * @return the factory bean name.
	 */
	String factory() default "";

	/**
	 * The {@link io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointMode} to be used for this endpoint. If not
	 * specified, the mode defined on the container factory is used.
	 * @return the checkpoint mode.
	 */
	String checkpointMode() default "";

	/**
	 * The {@link io.awspring.cloud.kinesis.listener.retrieval.RetrievalMode} to be used for this endpoint. If not
	 * specified, the mode defined on the container factory is used.
	 * @return the retrieval mode.
	 */
	String retrievalMode() default "";

	/**
	 * The {@link software.amazon.kinesis.common.InitialPositionInStream} to be used for this endpoint when no
	 * checkpoint exists yet for a shard, {@code TRIM_HORIZON} by default. Only {@code LATEST} and {@code TRIM_HORIZON}
	 * are supported here and the value always takes precedence over the position configured for the container factory.
	 * To fall back to the factory (for example to start {@code AT_TIMESTAMP}, which needs a timestamp the annotation
	 * cannot carry), set this attribute to an empty string.
	 * @return the initial position in stream.
	 */
	String initialPositionInStream() default "TRIM_HORIZON";

	/**
	 * The pre-registered enhanced fan-out consumer to read this stream with, given either as its name or as its ARN
	 * (only relevant for {@code ENHANCED_FAN_OUT} retrieval). A value starting with {@code arn:} is treated as an ARN,
	 * anything else as a name. A consumer belongs to a single stream, which is why it is declared per listener instead
	 * of through the {@code spring.cloud.aws.kinesis.listener} properties. If not specified, a consumer derived from
	 * the {@link #applicationName()} is registered or reused.
	 * @return the enhanced fan-out consumer name or ARN.
	 */
	String consumerName() default "";

	/**
	 * The DynamoDB lease table name for this listener, holding its shard leases and checkpoints. A lease table must
	 * never be shared between listeners, since KCL workers of different applications would then compete for each
	 * other's leases, which is why it is declared per listener instead of through the
	 * {@code spring.cloud.aws.kinesis.listener} properties. If not specified, the {@link #applicationName()} is used.
	 * @return the lease table name.
	 */
	String leaseTableName() default "";

	/**
	 * The CloudWatch namespace the KCL metrics of this listener are published to. If not specified, the namespace
	 * defined for the container factory is used, which in turn defaults to the {@link #applicationName()}.
	 * @return the metrics namespace.
	 */
	String metricsNamespace() default "";

}
