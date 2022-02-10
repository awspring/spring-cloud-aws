/*
 * Copyright 2013-2020 the original author or authors.
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

package io.awspring.cloud.v3.it.core.env.stack;

import io.awspring.cloud.v3.it.AWSIntegration;
import io.awspring.cloud.v3.it.support.TestStackEnvironment;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Agim Emruli
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@AWSIntegration
@Disabled("This functionality has been only implemented in XML configuration")
public class StackResourceUserTagsAwsTest {

	@Value("#{stackTags['tag1']}")
	private String stackTag1;

	@Value("#{stackTags['tag2']}")
	private String stackTag2;

	@Autowired
	private TestStackEnvironment testStackEnvironment;

	@Test
	void getObject_retrieveAttributesOfStackStartedByTestEnvironment_returnsStackUserTags() throws Exception {
		if (this.testStackEnvironment.isStackCreatedAutomatically()) {
			assertThat(this.stackTag1).isEqualTo("value1");
			assertThat(this.stackTag2).isEqualTo("value2");
		}
	}

}
