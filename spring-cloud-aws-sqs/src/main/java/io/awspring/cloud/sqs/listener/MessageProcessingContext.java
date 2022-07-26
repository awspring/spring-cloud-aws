package io.awspring.cloud.sqs.listener;


import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import org.springframework.messaging.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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

	public MessageProcessingContext(List<AsyncMessageInterceptor<T>> interceptors, Runnable backPressureReleaseCallback) {
		this.interceptors = Collections.unmodifiableList(interceptors);
		this.backPressureReleaseCallback = backPressureReleaseCallback;
	}

	public MessageProcessingContext<T> addInterceptor(AsyncMessageInterceptor<T> interceptor) {
		List<AsyncMessageInterceptor<T>> interceptors = new ArrayList<>(this.interceptors);
		interceptors.add(interceptor);
		return new MessageProcessingContext<>(interceptors, this.backPressureReleaseCallback);
	}

	public List<AsyncMessageInterceptor<T>> getInterceptors() {
		return this.interceptors;
	}

	public MessageProcessingContext<T> setBackPressureReleaseCallback(Runnable backPressureReleaseCallback) {
		return new MessageProcessingContext<>(this.interceptors, backPressureReleaseCallback);
	}

	public void executeBackPressureReleaseCallback() {
		this.backPressureReleaseCallback.run();
	}

	public static <T> MessageProcessingContext<T> create() {
		return new MessageProcessingContext<>(Collections.emptyList(), () -> {});
	}
}
