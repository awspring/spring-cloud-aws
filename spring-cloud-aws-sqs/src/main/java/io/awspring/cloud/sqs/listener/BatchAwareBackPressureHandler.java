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
package io.awspring.cloud.sqs.listener;

/**
 * {@link BackPressureHandler} specialization that allows requesting and releasing batches. Batch size should be
 * configured by the implementations.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface BatchAwareBackPressureHandler extends BackPressureHandler {

	/**
	 * Request a batch of permits.
	 * @return the number of permits acquired.
	 * @throws InterruptedException if the Thread is interrupted while waiting for permits.
	 */
	int requestBatch() throws InterruptedException;

	/**
	 * Release a batch of permits. This has the semantics of letting the {@link BackPressureHandler} know that all
	 * permits from a batch are being released, in opposition to {@link #release(int)} in which any number of permits
	 * can be specified.
	 */
	void releaseBatch();

	/**
	 * Return the configured batch size for this handler.
	 * @return the batch size.
	 */
	int getBatchSize();

}
