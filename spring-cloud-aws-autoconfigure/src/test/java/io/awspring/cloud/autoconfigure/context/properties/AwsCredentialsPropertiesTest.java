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

package org.springframework.cloud.aws.autoconfigure.context.properties;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AwsCredentialsProperties}.
 *
 * @author Tom Gianos
 * @author Maciej Walkowiak
 * @since 2.0.2
 */
class AwsCredentialsPropertiesTest {

	private AwsCredentialsProperties properties;

	@BeforeEach
	void setup() {
		this.properties = new AwsCredentialsProperties();
	}

	@Test
	void accessKeyCanBeSet() {
		assertThat(this.properties.getAccessKey()).as("Access key default value expected to be null").isNull();

		String newAccessKey = UUID.randomUUID().toString();
		this.properties.setAccessKey(newAccessKey);
		assertThat(this.properties.getAccessKey()).as("Access key should have been assigned").isEqualTo(newAccessKey);
	}

	@Test
	void secretKeyCanBeSet() {
		assertThat(this.properties.getSecretKey()).as("Secret key default value expected to be null").isNull();

		String newSecretKey = UUID.randomUUID().toString();
		this.properties.setSecretKey(newSecretKey);
		assertThat(this.properties.getSecretKey()).as("Secret key should have been assigned").isEqualTo(newSecretKey);
	}

	@Test
	void instanceProfileCanBeSet() {
		assertThat(this.properties.isInstanceProfile()).as("Instance profile default expected to be false").isFalse();

		this.properties.setInstanceProfile(true);
		assertThat(this.properties.isInstanceProfile()).as("Instance profile should have been assigned").isTrue();
	}

	@Test
	void profileNameCanBeSet() {
		assertThat(this.properties.getProfileName()).as("Default profile name is not expected to be set")
				.isEqualTo(null);

		String newProfileName = UUID.randomUUID().toString();
		this.properties.setProfileName(newProfileName);
		assertThat(this.properties.getProfileName()).as("Profile name should have been assigned")
				.isEqualTo(newProfileName);
	}

	@Test
	void profilePathCanBeSet() {
		assertThat(this.properties.getProfilePath()).as("Profile path default value expected to be null").isNull();

		String newProfilePath = UUID.randomUUID().toString();
		this.properties.setProfilePath(newProfilePath);
		assertThat(this.properties.getProfilePath()).as("Profile path should have been assigned")
				.isEqualTo(newProfilePath);
	}

}
