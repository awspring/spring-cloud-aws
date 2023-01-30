/*
 * Copyright 2013-2023 the original author or authors.
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
import java.util.stream.Collectors;
import org.springframework.util.Assert;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.Topic;

/**
 * @author Matej Nedić
 * @since 3.0.0
 */
public class ListTopicArnResolverCached implements TopicArnResolver {

	private final SnsClient snsClient;

	private final ConcurrentHashMap<String, Arn> cache = new ConcurrentHashMap<>();

	public ListTopicArnResolverCached(SnsClient snsClient) {
		this.snsClient = snsClient;
		resolveByCallingPaginator();
	}

	/**
	 * Resolves topic ARN by topic name. If topicName is already an ARN, it returns {@link Arn}. If topicName is just a
	 * string with a topic name, it lists all of topics or if topic already exists, just returns its ARN.
	 */
	@Override
	public Arn resolveTopicArn(String topicName) {
		Assert.notNull(topicName, "topicName must not be null");
		if (topicName.toLowerCase().startsWith("arn:")) {
			return Arn.fromString(topicName);
		}
		else if (cache.get(topicName) != null) {
			return cache.get(topicName);
		}
		else {
			resolveByCallingPaginator();
			return cache.get(topicName);
		}
	}

	private void resolveByCallingPaginator() {
		Map<String, Arn> nameAndArn = snsClient.listTopicsPaginator().topics().stream().collect(
				Collectors.toMap(t -> t.topicArn().substring(t.topicArn().lastIndexOf(":") + 1), topic -> Arn.fromString(topic.topicArn())));
		nameAndArn.forEach((key, value) -> cache.merge(key, value, (v1, v2) -> v2));
	}

	public int cacheSize() {
		return cache.size();
	}

}
