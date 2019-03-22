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

package org.springframework.cloud.aws.core.env.stack;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.support.TestStackEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Agim Emruli
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class StackResourceUserTagsAwsTest {

	@Value("#{stackTags['tag1']}")
	private String stackTag1;

	@Value("#{stackTags['tag2']}")
	private String stackTag2;

	@Autowired
	private TestStackEnvironment testStackEnvironment;

	@Test
	public void getObject_retrieveAttributesOfStackStartedByTestEnvironment_returnsStackUserTags()
			throws Exception {
		if (this.testStackEnvironment.isStackCreatedAutomatically()) {
			Assert.assertEquals("value1", this.stackTag1);
			Assert.assertEquals("value2", this.stackTag2);
		}
	}

}
