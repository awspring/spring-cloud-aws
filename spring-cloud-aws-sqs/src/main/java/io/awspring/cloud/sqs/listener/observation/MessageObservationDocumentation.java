/*
 * Copyright 2013-2024 the original author or authors.
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
package io.awspring.cloud.sqs.listener.observation;

import io.awspring.cloud.sqs.listener.sink.MessageProcessingPipelineSink;
import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;
import org.springframework.messaging.Message;

/**
 * Documented {@link io.micrometer.common.KeyValue KeyValues} for the observations on
 * {@link MessageProcessingPipelineSink processing} of {@link Message SQS messages}.
 *
 * @author Mariusz Sondecki
 */
public enum MessageObservationDocumentation implements ObservationDocumentation {

	SINGLE_MESSAGE_PROCESS {
		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return DefaultSingleMessageObservationConvention.class;
		}

		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return HighCardinalityKeyNames.values();
		}

		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityKeyNames.values();
		}
	},
	BATCH_MESSAGE_PROCESS {
		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return DefaultBatchMessageObservationConvention.class;
		}

		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return HighCardinalityKeyNames.values();
		}

		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityKeyNames.values();
		}
	};

	public enum HighCardinalityKeyNames implements KeyName {

		MESSAGE_ID {
			@Override
			public String asString() {
				return "message.id";
			}
		}

	}

	public enum LowCardinalityKeyNames implements KeyName {
		PROCESSING_MODE {
			public String asString() {
				return "messaging.processing.mode";
			}
		};
	}
}
