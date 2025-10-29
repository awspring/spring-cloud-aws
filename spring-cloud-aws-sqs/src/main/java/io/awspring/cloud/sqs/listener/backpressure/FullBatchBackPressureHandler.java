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
package io.awspring.cloud.sqs.listener.backpressure;

import io.awspring.cloud.sqs.listener.IdentifiableContainerComponent;
import io.awspring.cloud.sqs.listener.source.PollingMessageSource;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * Non-blocking {@link BackPressureHandler} implementation that ensures the exact batch size is requested.
 * <p>
 * If the amount of permits being requested is not equal to the batch size, permits will be limited to {@literal 0}. For
 * this limiting mechanism to work, the {@link FullBatchBackPressureHandler} must be used in combination with another
 * {@link BackPressureHandler} and be the last one in the chain of the {@link CompositeBackPressureHandler}
 *
 * <p>
 * This handler builds on the original <a href=
 * "https://github.com/awspring/spring-cloud-aws/blob/v3.4.0/spring-cloud-aws-sqs/src/main/java/io/awspring/cloud/sqs/listener/SemaphoreBackPressureHandler.java">
 * SemaphoreBackPressureHandler</a>, separating specific responsibilities into a more modular form and enabling
 * composition with other handlers as part of an extensible backpressure strategy.
 *
 * @see PollingMessageSource
 * @see CompositeBackPressureHandler
 *
 * @author Loïc Rouchon
 * @author Tomaz Fernandes
 *
 * @since 4.0.0
 */
public class FullBatchBackPressureHandler implements BatchAwareBackPressureHandler, IdentifiableContainerComponent {

	private static final Logger logger = LoggerFactory.getLogger(FullBatchBackPressureHandler.class);

	private final int batchSize;

	private String id = getClass().getSimpleName();

	private FullBatchBackPressureHandler(Builder builder) {
		this.batchSize = builder.batchSize;
		logger.debug("FullBatchBackPressureHandler created with configuration: batchSize: {}", this.batchSize);
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public int requestBatch() throws InterruptedException {
		return request(this.batchSize);
	}

	@Override
	public int request(int amount) throws InterruptedException {
		if (amount == batchSize) {
			return amount;
		}
		logger.warn("[{}] Could not acquire a full batch ({} / {}), cancelling current poll", this.id, amount,
				this.batchSize);
		return 0;
	}

	@Override
	public void release(int amount, ReleaseReason reason) {
		// NO-OP
	}

	@Override
	public boolean drain(Duration timeout) {
		return true;
	}

	public static class Builder {

		private int batchSize;

		public Builder batchSize(int batchSize) {
			this.batchSize = batchSize;
			return this;
		}

		public FullBatchBackPressureHandler build() {
			Assert.notNull(this.batchSize, "Missing configuration for batch size");
			Assert.isTrue(this.batchSize > 0, "The batch size must be greater than 0");
			return new FullBatchBackPressureHandler(this);
		}
	}
}
