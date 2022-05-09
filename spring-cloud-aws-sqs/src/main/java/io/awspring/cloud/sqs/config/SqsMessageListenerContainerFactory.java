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
package io.awspring.cloud.sqs.config;

import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import io.awspring.cloud.sqs.listener.interceptor.MessageVisibilityExtenderInterceptor;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * {@link MessageListenerContainerFactory} implementation for creating {@link SqsMessageListenerContainer} instances.
 *
 * @param <T> the {@link Message} payload type.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsMessageListenerContainerFactory<T>
		extends AbstractMessageListenerContainerFactory<T, SqsMessageListenerContainer<T>> {

	private static final Logger logger = LoggerFactory.getLogger(SqsMessageListenerContainerFactory.class);

	private Supplier<SqsAsyncClient> sqsAsyncClientSupplier;

	/**
	 * Create an instance with default {@link ContainerOptions}.
	 */
	public SqsMessageListenerContainerFactory() {
		this(ContainerOptions.create());
	}

	/**
	 * Create an instance with the provided {@link ContainerOptions}. Note that a copy of these options will be made, so
	 * any further change to the original options will have no effect on this factory.
	 * @param containerOptions the container options instance.
	 */
	private SqsMessageListenerContainerFactory(ContainerOptions containerOptions) {
		super(containerOptions);
	}

	@Override
	protected SqsMessageListenerContainer<T> createContainerInstance(Endpoint endpoint,
			ContainerOptions containerOptions) {
		logger.debug("Creating {} for endpoint {}", SqsMessageListenerContainer.class.getSimpleName(), endpoint);
		Assert.notNull(this.sqsAsyncClientSupplier, "No asyncClient set");
		SqsAsyncClient asyncClient = this.sqsAsyncClientSupplier.get();
		return new SqsMessageListenerContainer<>(asyncClient, configureContainerOptions(endpoint, containerOptions));
	}

	private ContainerOptions configureContainerOptions(Endpoint endpoint, ContainerOptions options) {
		if (endpoint instanceof SqsEndpoint) {
			SqsEndpoint sqsEndpoint = (SqsEndpoint) endpoint;
			ConfigUtils.INSTANCE
					.acceptIfNotNull(sqsEndpoint.getMaxInflightMessagesPerQueue(), options::maxInflightMessagesPerQueue)
					.acceptIfNotNull(sqsEndpoint.getPollTimeout(), options::pollTimeout)
					.acceptIfNotNull(sqsEndpoint.getMinimumVisibility(), this::addVisibilityExtender);
		}
		return options;
	}

	/**
	 * Set a supplier for {@link SqsAsyncClient} instances. A new instance will be used for each container created by
	 * this factory. Useful for high throughput containers where sharing an {@link SqsAsyncClient} would be harmful to
	 * performance.
	 *
	 * @param sqsAsyncClientSupplier the supplier.
	 */
	public void setSqsAsyncClientSupplier(Supplier<SqsAsyncClient> sqsAsyncClientSupplier) {
		Assert.notNull(sqsAsyncClientSupplier, "sqsAsyncClientSupplier cannot be null.");
		this.sqsAsyncClientSupplier = sqsAsyncClientSupplier;
	}

	/**
	 * Set the {@link SqsAsyncClient} instance to be shared by the containers. Useful for not-so-high throughput
	 * scenarios or when the client is tuned for more than the default maximum connections.
	 * @param sqsAsyncClient the client instance.
	 */
	public void setSqsAsyncClient(SqsAsyncClient sqsAsyncClient) {
		Assert.notNull(sqsAsyncClient, "sqsAsyncClient cannot be null.");
		setSqsAsyncClientSupplier(() -> sqsAsyncClient);
	}

	private void addVisibilityExtender(Integer minTimeToProcess) {
		MessageVisibilityExtenderInterceptor<T> interceptor = new MessageVisibilityExtenderInterceptor<>();
		interceptor.setMinimumVisibility(minTimeToProcess);
		super.addAsyncMessageInterceptor(interceptor);
	}

}
