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

import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representation of a processing context that can be used for communication between components. This class is immutable
 * and thread-safe.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class MessageProcessingContext<T> {

	private final List<AsyncMessageInterceptor<T>> interceptors;

	private final Runnable backPressureReleaseCallback;

	private final AcknowledgementCallback<T> acknowledgementCallback;

	private MessageProcessingContext(List<AsyncMessageInterceptor<T>> interceptors,
			Runnable backPressureReleaseCallback, AcknowledgementCallback<T> acknowledgementCallback) {
		this.interceptors = Collections.unmodifiableList(interceptors);
		this.backPressureReleaseCallback = backPressureReleaseCallback;
		this.acknowledgementCallback = acknowledgementCallback;
	}

	public MessageProcessingContext<T> addInterceptor(AsyncMessageInterceptor<T> interceptor) {
		List<AsyncMessageInterceptor<T>> contextInterceptors = new ArrayList<>(this.interceptors);
		contextInterceptors.add(interceptor);
		return new MessageProcessingContext<>(contextInterceptors, this.backPressureReleaseCallback,
				this.acknowledgementCallback);
	}

	public MessageProcessingContext<T> setBackPressureReleaseCallback(Runnable backPressureReleaseCallback) {
		return new MessageProcessingContext<>(this.interceptors, backPressureReleaseCallback,
				this.acknowledgementCallback);
	}

	public MessageProcessingContext<T> setAcknowledgmentCallback(AcknowledgementCallback<T> acknowledgementCallback) {
		return new MessageProcessingContext<>(this.interceptors, this.backPressureReleaseCallback,
				acknowledgementCallback);
	}

	public List<AsyncMessageInterceptor<T>> getInterceptors() {
		return this.interceptors;
	}

	public void runBackPressureReleaseCallback() {
		this.backPressureReleaseCallback.run();
	}

	public AcknowledgementCallback<T> getAcknowledgmentCallback() {
		return this.acknowledgementCallback;
	}

	public static <T> MessageProcessingContext<T> create() {
		return new MessageProcessingContext<>(Collections.emptyList(), () -> {
		}, new AcknowledgementCallback<T>() {
		});
	}
}
