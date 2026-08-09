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
package io.awspring.cloud.sqs.integration;

import java.util.Comparator;
import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;

/**
 * Starts Localstack and schedules the classes that do not need it first, so they run while it comes up instead of
 * queueing behind the classes that wait for it. Ordering runs during discovery, before any test, which is also the
 * earliest point at which the container can be asked for.
 */
public class LocalStackAwareClassOrderer implements ClassOrderer {

	@Override
	public void orderClasses(ClassOrdererContext context) {
		if (context.getClassDescriptors().stream().anyMatch(descriptor -> needsContainer(descriptor) == 1)) {
			BaseSqsIntegrationTest.startAsync();
		}
		context.getClassDescriptors().sort(Comparator.comparingInt(LocalStackAwareClassOrderer::needsContainer));
	}

	private static int needsContainer(ClassDescriptor descriptor) {
		return BaseSqsIntegrationTest.class.isAssignableFrom(descriptor.getTestClass()) ? 1 : 0;
	}

}
