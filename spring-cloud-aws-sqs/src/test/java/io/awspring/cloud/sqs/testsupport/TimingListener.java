/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.sqs.testsupport;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TimingListener implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

	private static final long START = System.currentTimeMillis();
	private static final AtomicInteger RUNNING = new AtomicInteger();

	@Override
	public void beforeTestExecution(ExtensionContext context) {
		System.out.printf("TIMING %6d START inflight=%d %s.%s%n", System.currentTimeMillis() - START,
				RUNNING.incrementAndGet(), context.getRequiredTestClass().getSimpleName(),
				context.getRequiredTestMethod().getName());
	}

	@Override
	public void afterTestExecution(ExtensionContext context) {
		System.out.printf("TIMING %6d END   inflight=%d %s.%s%n", System.currentTimeMillis() - START,
				RUNNING.decrementAndGet(), context.getRequiredTestClass().getSimpleName(),
				context.getRequiredTestMethod().getName());
	}
}
