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
package io.awspring.cloud.kinesis.listener.retrieval;

import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 *
 * @author Matej Nedic
 * @since 4.2.0
 */
public final class KinesisConsumerResolver {

	private static final String ARN_PREFIX = "arn:";

	private KinesisConsumerResolver() {
	}

	@Nullable
	public static String resolveConsumerArn(String nameOrArn) {
		return isArn(nameOrArn) ? nameOrArn : null;
	}

	@Nullable
	public static String resolveConsumerName(String nameOrArn) {
		return isArn(nameOrArn) ? null : nameOrArn;
	}

	public static boolean isArn(String nameOrArn) {
		Assert.hasText(nameOrArn, "consumer name or ARN must not be empty");
		return nameOrArn.toLowerCase(Locale.ROOT).startsWith(ARN_PREFIX);
	}

}
