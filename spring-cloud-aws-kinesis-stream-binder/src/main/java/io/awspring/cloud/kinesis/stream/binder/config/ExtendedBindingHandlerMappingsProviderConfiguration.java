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
package io.awspring.cloud.kinesis.stream.binder.config;

import java.util.Map;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.cloud.stream.config.BindingHandlerAdvise.MappingsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for extended binding metadata.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */

@Configuration(proxyBeanMethods = false)
public class ExtendedBindingHandlerMappingsProviderConfiguration {

	@Bean
	public MappingsProvider kinesisExtendedPropertiesDefaultMappingsProvider() {
		return () -> Map.of(ConfigurationPropertyName.of("spring.cloud.stream.kinesis.bindings"),
				ConfigurationPropertyName.of("spring.cloud.stream.kinesis.default"));
	}

}
