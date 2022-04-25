/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.sns.sms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Default implementation of Converter that uses {@link ObjectMapper} to convert from object to String.
 * @author Matej Nedic
 */
public class DefaultConverter implements Converter {

	private final ObjectMapper objectMapper;
	private final Log log = LogFactory.getLog(DefaultConverter.class);

	public DefaultConverter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public String convert(Object payload) {
		if (payload instanceof String) {
			return (String) payload;
		}
		try {
			return objectMapper.writeValueAsString(payload);
		}
		catch (JsonProcessingException e) {
			log.error("Exception happened while trying to deserialize Sms payload to json", e);
			throw new RuntimeException(e);
		}
	}

}
