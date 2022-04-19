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
package io.awspring.cloud.sns.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.arns.Arn;

/**
 * Caching implementation for resolving ARN from topicName. It is delegating work to {@link TopicArnResolver}
 * implementation, meaning it will only cache Arn for given topic name and return it.
 *
 * @author Matej Nedic
 * @since 3.0.0
 */
public class CachingTopicArnResolver implements TopicArnResolver {
	private final TopicArnResolver delegate;
	private final Map<String, Arn> cache = new ConcurrentHashMap<>();

	public CachingTopicArnResolver(TopicArnResolver topicArnResolver) {
		this.delegate = topicArnResolver;
	}

	@Override
	public Arn resolveTopicArn(String topicName) {
		if (topicName.toLowerCase().startsWith("arn:")) {
			return delegate.resolveTopicArn(topicName);
		}

		return cache.computeIfAbsent(topicName, delegate::resolveTopicArn);
	}
}
