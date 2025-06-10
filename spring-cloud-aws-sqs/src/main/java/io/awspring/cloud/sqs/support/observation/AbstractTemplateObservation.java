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

import io.awspring.cloud.sqs.operations.SendResult;
import io.micrometer.common.KeyValues;
import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;
import io.micrometer.observation.transport.SenderContext;
import java.util.HashMap;
import java.util.Map;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Observation for {@link io.awspring.cloud.sqs.operations.MessagingOperations}.
 *
 * @author Tomaz Fernandes
 * @since 3.4
 */
public abstract class AbstractTemplateObservation {

	/**
	 * {@link AbstractTemplateObservation.Convention} for template key values.
	 */
	public static abstract class Convention<ContextType extends Context> implements ObservationConvention<ContextType> {

		@Override
		@NonNull
		public KeyValues getLowCardinalityKeyValues(ContextType context) {
			return KeyValues
					.of(AbstractTemplateObservation.Documentation.LowCardinalityTags.MESSAGING_SYSTEM
							.withValue(getMessagingSystem()),
							AbstractTemplateObservation.Documentation.LowCardinalityTags.MESSAGING_OPERATION
									.withValue("publish"),
							AbstractTemplateObservation.Documentation.LowCardinalityTags.MESSAGING_DESTINATION_NAME
									.withValue(context.getDestinationName()),
							AbstractTemplateObservation.Documentation.LowCardinalityTags.MESSAGING_DESTINATION_KIND
									.withValue(getSourceKind()))
					.and(getSpecificLowCardinalityKeyValues(context)).and(getCustomLowCardinalityKeyValues(context));
		}

		protected KeyValues getSpecificLowCardinalityKeyValues(ContextType context) {
			return KeyValues.empty();
		}

		/**
		 * Return custom low cardinality key values for the observation. This method is intended to be overridden by
		 * subclasses to add custom low cardinality tags to the observation.
		 * 
		 * @param context the context for which to get key values.
		 * @return key values to add to the observation, empty by default.
		 */
		protected KeyValues getCustomLowCardinalityKeyValues(ContextType context) {
			return KeyValues.empty();
		}

		@Override
		@NonNull
		public KeyValues getHighCardinalityKeyValues(@NonNull ContextType context) {
			return getMessageIdKeyValue(context).and(getSpecificHighCardinalityKeyValues(context))
					.and(getCustomHighCardinalityKeyValues(context));
		}

		private KeyValues getMessageIdKeyValue(ContextType context) {
			String messageId = context.getMessageId();
			return messageId != null ? KeyValues.of(Documentation.HighCardinalityTags.MESSAGE_ID.withValue(messageId))
					: KeyValues.empty();
		}

		protected KeyValues getSpecificHighCardinalityKeyValues(ContextType context) {
			return KeyValues.empty();
		}

		/**
		 * Return custom high cardinality key values for the observation. This method is intended to be overridden by
		 * subclasses to add custom high cardinality tags to the observation.
		 * 
		 * @param context the context for which to get key values.
		 * @return key values to add to the observation, empty by default.
		 */
		protected KeyValues getCustomHighCardinalityKeyValues(ContextType context) {
			return KeyValues.empty();
		}

		@Override
		public String getContextualName(ContextType context) {
			return context.getDestinationName() + " send";
		}

		@Override
		public String getName() {
			return "spring.aws." + getMessagingSystem() + ".template";
		}

		protected abstract String getSourceKind();

		protected abstract String getMessagingSystem();

	}

	/**
	 * {@link ObservationDocumentation} for template keys.
	 */
	public static abstract class Documentation implements ObservationDocumentation {

		KeyName[] EMPTY = new KeyName[0];

		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return getSpecificDefaultConvention();
		}

		protected abstract Class<? extends ObservationConvention<? extends Observation.Context>> getSpecificDefaultConvention();

		@Override
		@NonNull
		public KeyName[] getLowCardinalityKeyNames() {
			return KeyName.merge(AbstractTemplateObservation.Documentation.LowCardinalityTags.values(),
					getSpecificLowCardinalityKeyNames());
		}

		protected KeyName[] getSpecificLowCardinalityKeyNames() {
			return EMPTY;
		}

		@Override
		@NonNull
		public KeyName[] getHighCardinalityKeyNames() {
			return KeyName.merge(AbstractTemplateObservation.Documentation.LowCardinalityTags.values(),
					getSpecificHighCardinalityKeyNames());
		}

		protected KeyName[] getSpecificHighCardinalityKeyNames() {
			return EMPTY;
		}

		/**
		 * Low cardinality tags.
		 */
		public enum LowCardinalityTags implements KeyName {

			/**
			 * Messaging system.
			 */
			MESSAGING_SYSTEM {
				@Override
				@NonNull
				public String asString() {
					return "messaging.system";
				}

			},

			/**
			 * Messaging operation.
			 */
			MESSAGING_OPERATION {
				@Override
				@NonNull
				public String asString() {
					return "messaging.operation";
				}

			},

			/**
			 * Messaging destination name.
			 */
			MESSAGING_DESTINATION_NAME {
				@Override
				@NonNull
				public String asString() {
					return "messaging.destination.name";
				}

			},

			/**
			 * Messaging destination kind.
			 */
			MESSAGING_DESTINATION_KIND {
				@Override
				@NonNull
				public String asString() {
					return "messaging.destination.kind";
				}

			},

		}

		/**
		 * High cardinality tags.
		 */
		public enum HighCardinalityTags implements KeyName {

			/**
			 * Message id.
			 */
			MESSAGE_ID {
				@Override
				@NonNull
				public String asString() {
					return "messaging.message.id";
				}
			},

		}

	}

	/**
	 * {@link SenderContext} for message listeners.
	 */
	public static abstract class Context extends SenderContext<Map<String, Object>> {

		private static final String BAGGAGE_KEY = "baggage";
		private static final String TRACEPARENT_KEY = "traceparent";
		private static final String TRACESTATE_KEY = "tracestate";
		private static final String B3_KEY = "b3";

		private final Message<?> message;
		private final String destinationName;
		private SendResult<?> sendResult;

		/**
		 * Build a messaging sender context.
		 *
		 * @param message the message.
		 * @param destinationName the destination name.
		 */
		protected Context(Message<?> message, String destinationName) {
			super((carrier, key, value) -> {
				if (carrier != null && isAllowedKey(key)) {
					carrier.put(key, value);
				}
			});
			setCarrier(new HashMap<>());
			Assert.notNull(message, "Message must not be null");
			Assert.notNull(destinationName, "destinationName must not be null");
			this.message = message;
			this.destinationName = destinationName;
		}

		private static boolean isAllowedKey(String key) {
			return BAGGAGE_KEY.equals(key) || TRACEPARENT_KEY.equals(key) || TRACESTATE_KEY.equals(key)
					|| B3_KEY.equals(key);
		}

		/**
		 * Set the send result for this context.
		 * 
		 * @param sendResult the send result from message sending operation.
		 */
		public void setSendResult(SendResult<?> sendResult) {
			Assert.notNull(sendResult, "sendResult must not be null");
			this.sendResult = sendResult;
		}

		/**
		 * Return the message id.
		 *
		 * @return the message id.
		 */
		public String getMessageId() {
			return this.sendResult != null ? sendResult.messageId().toString() : null;
		}

		/**
		 * Return the send result.
		 * 
		 * @return the send result.
		 */
		public SendResult<?> getSendResult() {
			return this.sendResult;
		}

		/**
		 * Return the message.
		 *
		 * @return the message.
		 */
		public Message<?> getMessage() {
			return this.message;
		}

		/**
		 * Return the destination name.
		 *
		 * @return the destination name.
		 */
		public String getDestinationName() {
			return this.destinationName;
		}

	}

	/**
	 * Contains observation-related instances that are specific to a messaging system.
	 *
	 * @param <ContextType>
	 */
	public interface Specifics<ContextType extends Context> {

		/**
		 * Construct an specific {@link AbstractListenerObservation.Context} instance with the provided message.
		 *
		 * @param message the message from which the context should be constructed.
		 * @return the context instance.
		 */
		ContextType createContext(Message<?> message, String destinationName);

		/**
		 * Return the default {@link ObservationConvention} for the specific messaging system.
		 *
		 * @return the convention.
		 */
		ObservationConvention<ContextType> getDefaultConvention();

		/**
		 * Return the {@link ObservationDocumentation} for the specific messaging system.
		 *
		 * @return the documentation.
		 */
		ObservationDocumentation getDocumentation();

	}

}
