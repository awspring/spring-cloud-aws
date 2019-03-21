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

package org.springframework.cloud.aws.messaging;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;

/**
 * @author Alain Sahli
 */
abstract class AbstractContainerTest {

	@Autowired
	protected SimpleMessageListenerContainer simpleMessageListenerContainer;

	@Before
	public void setUp() throws Exception {
		if (!this.simpleMessageListenerContainer.isRunning()) {
			this.simpleMessageListenerContainer.start();
		}
	}

	@After
	public void tearDown() throws Exception {
		if (this.simpleMessageListenerContainer.isRunning()) {
			CountDownLatch countDownLatch = new CountDownLatch(1);
			this.simpleMessageListenerContainer.stop(countDownLatch::countDown);

			if (!countDownLatch.await(15, TimeUnit.SECONDS)) {
				throw new Exception("Couldn't stop container withing 15 seconds");
			}
		}
	}

}
