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
package io.awspring.cloud.sqs.listener;

import java.time.Duration;
import java.util.List;

public class CompositeBackPressureHandler implements BatchAwareBackPressureHandler, IdentifiableContainerComponent {

	private final List<BackPressureHandler> backPressureHandlers;

	private final int batchSize;

	private String id;

	public CompositeBackPressureHandler(List<BackPressureHandler> backPressureHandlers, int batchSize) {
		this.backPressureHandlers = backPressureHandlers;
		this.batchSize = batchSize;
	}

	@Override
	public void setId(String id) {
		this.id = id;
		backPressureHandlers.stream().filter(IdentifiableContainerComponent.class::isInstance)
				.map(IdentifiableContainerComponent.class::cast)
				.forEach(bph -> bph.setId(bph.getClass().getSimpleName() + "-" + id));
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public int requestBatch() throws InterruptedException {
		return request(batchSize);
	}

	@Override
	public int request(int amount) throws InterruptedException {
		int obtained = amount;
		int[] obtainedPerBph = new int[backPressureHandlers.size()];
		for (int i = 0; i < backPressureHandlers.size() && obtained > 0; i++) {
			obtainedPerBph[i] = backPressureHandlers.get(i).request(obtained);
			obtained = Math.min(obtained, obtainedPerBph[i]);
		}
		for (int i = 0; i < backPressureHandlers.size(); i++) {
			int obtainedForBph = obtainedPerBph[i];
			if (obtainedForBph > obtained) {
				backPressureHandlers.get(i).release(obtainedForBph - obtained, ReleaseReason.LIMITED);
			}
		}
		return obtained;
	}

	@Override
	public void release(int amount, ReleaseReason reason) {
		for (BackPressureHandler handler : backPressureHandlers) {
			handler.release(amount, reason);
		}
	}

	@Override
	public boolean drain(Duration timeout) {
		boolean result = true;
		for (BackPressureHandler handler : backPressureHandlers) {
			result &= !handler.drain(timeout);
		}
		return result;
	}
}
