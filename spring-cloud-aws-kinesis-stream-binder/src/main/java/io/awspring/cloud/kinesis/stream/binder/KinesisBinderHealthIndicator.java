/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.kinesis.stream.binder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.LimitExceededException;

/**
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class KinesisBinderHealthIndicator implements HealthIndicator {

	private final KinesisMessageChannelBinder kinesisMessageChannelBinder;

	public KinesisBinderHealthIndicator(KinesisMessageChannelBinder kinesisMessageChannelBinder) {
		this.kinesisMessageChannelBinder = kinesisMessageChannelBinder;
	}

	@Override
	public Health health() {
		KinesisAsyncClient amazonKinesis = this.kinesisMessageChannelBinder.getAmazonKinesis();
		List<String> streamsInUse = new ArrayList<>(this.kinesisMessageChannelBinder.getStreamsInUse());
		for (String stream : streamsInUse) {
			while (true) {
				try {
					amazonKinesis.listShards(request -> request.streamName(stream).maxResults(1)).join();
					break;
				}
				catch (CompletionException ex) {
					Throwable cause = ex.getCause();
					if (cause instanceof LimitExceededException) {
						try {
							TimeUnit.SECONDS.sleep(1);
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return Health.down().withException(ex).build();
						}
					}
					else {
						return Health.down().withException(ex).build();
					}
				}
			}
		}
		return Health.up().build();
	}

}
