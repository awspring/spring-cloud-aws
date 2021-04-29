/*
 * Copyright 2013-2019 the original author or authors.
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

package io.awspring.cloud.cloudmap.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringCloudAwsCloudMapSample implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudAwsCloudMapSample.class);

	@Value("${test-namespace/test-service}")
	private String registryDetails;

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudAwsCloudMapSample.class, args);
	}

	@Override
	public void run(ApplicationArguments args) {
		LOGGER.info("CloudMap registry details: {}", registryDetails);
	}

}
