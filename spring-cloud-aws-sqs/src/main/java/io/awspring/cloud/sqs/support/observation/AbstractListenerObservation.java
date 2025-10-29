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
import io.micrometer.common.KeyValues;
import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;
import io.micrometer.observation.transport.ReceiverContext;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;

/**
 * Observation for Message Listener Containers.
 *
 * @author Tomaz Fernandes
 * @since 3.4
 */
public abstract class AbstractListenerObservation {

	/**
	 * {@link ObservationConvention} for message listener key values.
	 */
	public static abstract class Convention<ContextType extends Context> implements ObservationConvention<ContextType> {

		@Override
		@NonNull
		public KeyValues getLowCardinalityKeyValues(ContextType context) {
			return KeyValues
					.of(AbstractListenerObservation.Documentation.LowCardinalityTags.MESSAGING_SYSTEM
							.withValue(getMessagingSystem()),
							AbstractListenerObservation.Documentation.LowCardinalityTags.MESSAGING_OPERATION
									.withValue("receive"),
							AbstractListenerObservation.Documentation.LowCardinalityTags.MESSAGING_SOURCE_NAME
									.withValue(context.getSourceName()),
							AbstractListenerObservation.Documentation.LowCardinalityTags.MESSAGING_SOURCE_KIND
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
		public KeyValues getHighCardinalityKeyValues(ContextType context) {
			return KeyValues.of(Documentation.HighCardinalityTags.MESSAGE_ID.withValue(context.getMessageId()))
					.and(getSpecificHighCardinalityKeyValues(context)).and(getCustomHighCardinalityKeyValues(context));
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
			return context.getSourceName() + " receive";
		}

		@Override
		public String getName() {
			return "spring.aws." + getMessagingSystem() + ".listener";
		}

		protected abstract String getSourceKind();

		protected abstract String getMessagingSystem();

	}

	/**
	 * {@link ObservationDocumentation} for message listener keys.
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
			return KeyName.merge(AbstractListenerObservation.Documentation.LowCardinalityTags.values(),
					getSpecificLowCardinalityKeyNames());
		}

		protected KeyName[] getSpecificLowCardinalityKeyNames() {
			return EMPTY;
		}

		@Override
		@NonNull
		public KeyName[] getHighCardinalityKeyNames() {
			return KeyName.merge(HighCardinalityTags.values(), getSpecificHighCardinalityKeyNames());
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
			 * Messaging source name.
			 */
			MESSAGING_SOURCE_NAME {
				@Override
				@NonNull
				public String asString() {
					return "messaging.source.name";
				}

			},

			/**
			 * Messaging source kind.
			 */
			MESSAGING_SOURCE_KIND {
				@Override
				@NonNull
				public String asString() {
					return "messaging.source.kind";
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
	 * {@link ReceiverContext} for message listeners.
	 */
	public static abstract class Context extends ReceiverContext<Message<?>> {

		private final Message<?> message;
		private final String messageId;
		private final String sourceName;

		/**
		 * Build a messaging receiver context.
		 * @param message the message.
		 * @param sourceName the source name.
		 */
		protected Context(Message<?> message, String sourceName) {
			super((carrier, key) -> carrier.getHeaders().get(key, String.class));
			setCarrier(message);
			this.message = message;
			this.messageId = MessageHeaderUtils.getId(message);
			this.sourceName = sourceName;
		}

		/**
		 * Return the message id.
		 * @return the message id.
		 */
		public String getMessageId() {
			return this.messageId;
		}

		/**
		 * Return the message.
		 * @return the message.
		 */
		public Message<?> getMessage() {
			return this.message;
		}

		/**
		 * Return the source name.
		 * @return the source name.
		 */
		public String getSourceName() {
			return this.sourceName;
		}
	}

	/**
	 * Observation-related instances that are specific to a messaging system.
	 * @param <ContextType>
	 */
	public interface Specifics<ContextType extends Context> {

		/**
		 * Construct an specific {@link Context} instance with the provided message.
		 * @param message the message from which the context should be constructed.
		 * @return the context instance.
		 */
		ContextType createContext(Message<?> message);

		/**
		 * Return the default {@link ObservationConvention} for the specific messaging system.
		 * @return the convention.
		 */
		ObservationConvention<ContextType> getDefaultConvention();

		/**
		 * Return the {@link ObservationDocumentation} for the specific messaging system.
		 * @return the documentation.
		 */
		ObservationDocumentation getDocumentation();

	}

}
