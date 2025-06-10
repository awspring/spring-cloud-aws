/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.sqs.support.observation;

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.micrometer.common.KeyValues;
import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

/**
 * Observation for {@link io.awspring.cloud.sqs.operations.SqsTemplate}.
 *
 * @author Tomaz Fernandes
 * @since 3.4
 */
public class SqsTemplateObservation {

	/**
	 * {@link ObservationDocumentation} for SqsTemplate keys.
	 */
	public static class Documentation extends AbstractTemplateObservation.Documentation {

		@Override
		protected Class<? extends ObservationConvention<? extends Observation.Context>> getSpecificDefaultConvention() {
			return DefaultConvention.class;
		}

		protected KeyName[] getSpecificHighCardinalityKeyNames() {
			return SqsTemplateObservation.Documentation.HighCardinalityTags.values();
		}

		/**
		 * High cardinality tags.
		 */
		public enum HighCardinalityTags implements KeyName {

			/**
			 * Message group id for messages sent to FIFO queues.
			 */
			MESSAGE_GROUP_ID {
				@Override
				@NonNull
				public String asString() {
					return "messaging.message.message-group.id";
				}
			},

			/**
			 * Message deduplication id for messages sent to FIFO queues.
			 */
			MESSAGE_DEDUPLICATION_ID {
				@Override
				@NonNull
				public String asString() {
					return "messaging.message.message-deduplication.id";
				}
			},

		}

	}

	/**
	 * {@link ObservationConvention} for {@link SqsTemplate} key values.
	 */
	public interface Convention extends ObservationConvention<Context> {

		@Override
		default boolean supportsContext(@NonNull Observation.Context context) {
			return context instanceof Context;
		}

		/**
		 * Return the name of this observation.
		 *
		 * @return the name of the observation.
		 */
		@Override
		default String getName() {
			return "spring.cloud.aws.sqs.template";
		}

	}

	/**
	 * {@link SqsListenerObservation.Context} for {@link SqsTemplate}.
	 */
	public static class Context extends AbstractTemplateObservation.Context {

		private final String messageGroupId;

		private final String messageDeduplicationId;

		/**
		 * Construct an SQS message sender context.
		 *
		 * @param message the message.
		 */
		public Context(Message<?> message, String destinationName) {
			super(message, destinationName);
			this.messageGroupId = message.getHeaders()
					.containsKey(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER)
							? MessageHeaderUtils.getHeaderAsString(message,
									SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER)
							: "";
			this.messageDeduplicationId = message.getHeaders()
					.containsKey(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER)
							? MessageHeaderUtils.getHeaderAsString(message,
									SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER)
							: "";
			setRemoteServiceName("AWS SQS");
		}

		/**
		 * Return the message group id.
		 *
		 * @return the message group id.
		 */
		public String getMessageGroupId() {
			return this.messageGroupId;
		}

		/**
		 * Return the message deduplication id.
		 *
		 * @return the message deduplication id.
		 */
		public String getMessageDeduplicationId() {
			return this.messageDeduplicationId;
		}
	}

	/**
	 * {@link ObservationConvention} for SQS template key values.
	 *
	 * @author Tomaz Fernandes
	 * @since 3.4
	 */
	public static class DefaultConvention extends AbstractTemplateObservation.Convention<Context>
			implements Convention {

		@Override
		protected String getSourceKind() {
			return "queue";
		}

		@Override
		protected String getMessagingSystem() {
			return "sqs";
		}

		@Override
		protected KeyValues getSpecificHighCardinalityKeyValues(Context context) {
			// FIFO-specific keys
			String messageDeduplicationId = context.getMessageDeduplicationId();
			String messageGroupId = context.getMessageGroupId();
			return StringUtils.hasText(messageGroupId) && StringUtils.hasText(messageDeduplicationId) ? KeyValues.of(
					SqsTemplateObservation.Documentation.HighCardinalityTags.MESSAGE_GROUP_ID.withValue(messageGroupId),
					SqsTemplateObservation.Documentation.HighCardinalityTags.MESSAGE_DEDUPLICATION_ID
							.withValue(messageDeduplicationId))
					: KeyValues.empty();
		}

	}

	/**
	 * Observation-related instances that are specific to SQS.
	 */
	public static class SqsSpecifics implements AbstractTemplateObservation.Specifics<Context> {

		private static final DefaultConvention CONVENTION_INSTANCE = new DefaultConvention();

		private static final Documentation DOCUMENTATION_INSTANCE = new Documentation();

		@Override
		public Context createContext(Message<?> message, String destinationName) {
			return new Context(message, destinationName);
		}

		@Override
		public ObservationConvention<Context> getDefaultConvention() {
			return CONVENTION_INSTANCE;
		}

		@Override
		public ObservationDocumentation getDocumentation() {
			return DOCUMENTATION_INSTANCE;
		}
	}

}
