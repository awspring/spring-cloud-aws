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

package io.awspring.cloud.sqs.sample;

import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

@Component
class SampleListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(SampleListener.class);

	@SqsListener("InfrastructureStack-spring-aws")
	public void listenToMessage(String message) {
		LOGGER.info("This is message you want to see: {}", message);
	}

	@SqsListener("InfrastructureStack-aws-pojo")
	public void listenToPerson(Person person) {
		LOGGER.info(person.toString());
	}

}
