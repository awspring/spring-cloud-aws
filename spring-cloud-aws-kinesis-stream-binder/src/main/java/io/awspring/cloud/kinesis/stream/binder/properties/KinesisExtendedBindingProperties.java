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
package io.awspring.cloud.kinesis.stream.binder.properties;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;
import org.springframework.cloud.stream.binder.ExtendedBindingProperties;

/**
 * The extended Kinesis-specific binding configuration properties.
 *
 * @author Peter Oates
 * @author Artem Bilan
 *
 * @since 4.0
 */
@ConfigurationProperties("spring.cloud.stream.kinesis")
public class KinesisExtendedBindingProperties
		implements ExtendedBindingProperties<KinesisConsumerProperties, KinesisProducerProperties> {

	private static final String DEFAULTS_PREFIX = "spring.cloud.stream.kinesis.default";

	private Map<String, KinesisBindingProperties> bindings = new HashMap<>();

	public Map<String, KinesisBindingProperties> getBindings() {
		return this.bindings;
	}

	public void setBindings(Map<String, KinesisBindingProperties> bindings) {
		this.bindings = bindings;
	}

	@Override
	public KinesisConsumerProperties getExtendedConsumerProperties(String channelName) {
		if (this.bindings.containsKey(channelName) && this.bindings.get(channelName).getConsumer() != null) {
			return this.bindings.get(channelName).getConsumer();
		}
		else {
			return new KinesisConsumerProperties();
		}
	}

	@Override
	public KinesisProducerProperties getExtendedProducerProperties(String channelName) {
		KinesisBindingProperties kinesisBindingProperties = this.bindings.get(channelName);
		if (kinesisBindingProperties != null && kinesisBindingProperties.getProducer() != null) {
			return kinesisBindingProperties.getProducer();
		}
		else {
			return new KinesisProducerProperties();
		}
	}

	@Override
	public String getDefaultsPrefix() {
		return DEFAULTS_PREFIX;
	}

	@Override
	public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
		return KinesisBindingProperties.class;
	}

}
