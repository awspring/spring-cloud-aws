package io.awspring.cloud.sqs.listener;


import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import org.springframework.messaging.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Representation of a processing context that can be used for communication
 * between components.
 * This class is immutable and thread-safe.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class MessageProcessingContext<T> {

	private final List<AsyncMessageInterceptor<T>> interceptors;

	private final Runnable backPressureReleaseCallback;

	private final AcknowledgementCallback<T> acknowledgementCallback;

	private MessageProcessingContext(List<AsyncMessageInterceptor<T>> interceptors,
									 Runnable backPressureReleaseCallback,
									 AcknowledgementCallback<T> acknowledgementCallback) {
		this.interceptors = Collections.unmodifiableList(interceptors);
		this.backPressureReleaseCallback = backPressureReleaseCallback;
		this.acknowledgementCallback = acknowledgementCallback;
	}

	public MessageProcessingContext<T> addInterceptor(AsyncMessageInterceptor<T> interceptor) {
		List<AsyncMessageInterceptor<T>> contextInterceptors = new ArrayList<>(this.interceptors);
		contextInterceptors.add(interceptor);
		return new MessageProcessingContext<>(contextInterceptors, this.backPressureReleaseCallback, this.acknowledgementCallback);
	}

	public MessageProcessingContext<T> setBackPressureReleaseCallback(Runnable backPressureReleaseCallback) {
		return new MessageProcessingContext<>(this.interceptors, backPressureReleaseCallback, this.acknowledgementCallback);
	}

	public MessageProcessingContext<T> setAcknowledgmentCallback(AcknowledgementCallback<T> acknowledgementCallback) {
		return new MessageProcessingContext<>(this.interceptors, this.backPressureReleaseCallback, acknowledgementCallback);
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
		return new MessageProcessingContext<>(Collections.emptyList(), () -> {}, new AcknowledgementCallback<T>(){});
	}
}
