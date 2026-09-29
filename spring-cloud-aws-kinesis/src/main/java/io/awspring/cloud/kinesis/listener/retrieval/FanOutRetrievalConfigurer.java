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

import io.awspring.cloud.kinesis.listener.KclContainerOptions;
import java.util.Collection;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.retrieval.RetrievalSpecificConfig;
import software.amazon.kinesis.retrieval.fanout.FanOutConfig;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
public class FanOutRetrievalConfigurer implements KclRetrievalConfigurer {

	@Override
	public RetrievalSpecificConfig createRetrievalConfig(Collection<String> streamNames, String applicationName,
			KinesisAsyncClient kinesisClient, KclContainerOptions options) {
		Assert.isTrue(streamNames.size() == 1,
				"ENHANCED_FAN_OUT supports a single stream per listener, because an enhanced fan-out consumer is registered against one stream; use POLLING to consume "
						+ streamNames.size() + " streams with one listener, or declare one listener per stream");
		FanOutConfig fanOutConfig = new FanOutConfig(kinesisClient).streamName(streamNames.iterator().next())
				.applicationName(applicationName);
		if (options.getConsumerArn() != null) {
			fanOutConfig.consumerArn(options.getConsumerArn());
		}
		if (options.getConsumerName() != null) {
			fanOutConfig.consumerName(options.getConsumerName());
		}
		return fanOutConfig;
	}

}
