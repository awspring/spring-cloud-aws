package io.awspring.cloud.sqs;

import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Utility methods to handle {@link SmartLifecycle} hooks.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class LifecycleUtils {

	private static final SimpleAsyncTaskExecutor executor;

	static {
		executor = new SimpleAsyncTaskExecutor();
		executor.setThreadNamePrefix("lifecycle-thread-");
	}

	private LifecycleUtils() {
	}

	/**
	 * Execute the provided action if the provided objects
	 * are {@link SmartLifecycle} instances.
	 * @param action the action.
	 * @param objects the objects.
	 */
	public static void manageLifecycle(Consumer<SmartLifecycle> action, Object... objects) {
		Arrays.stream(objects).forEach(object -> {
			if (object instanceof SmartLifecycle) {
				action.accept((SmartLifecycle) object);
			} else if (object instanceof Collection) {
				((Collection<?>) object)
					.forEach(innerObject -> manageLifecycle(action, innerObject));
			}
		});
	}

	/**
	 * Execute the provided action if the provided objects
	 * are {@link SmartLifecycle} instances.
	 * @param action the action.
	 * @param objects the objects.
	 */
	public static void manageLifecycleParallel(Consumer<SmartLifecycle> action, Object... objects) {
		Arrays.stream(objects).forEach(object -> {
			if (object instanceof SmartLifecycle) {
				action.accept((SmartLifecycle) object);
			} else if (object instanceof Collection) {
				CompletableFuture
					.allOf(((Collection<?>) object).stream()
						.map(obj -> CompletableFuture.runAsync(() -> manageLifecycle(action, obj), executor))
						.toArray(CompletableFuture[]::new)).join();
			}
		});
	}

	/**
	 * Starts the provided objects that are a {@link SmartLifecycle} instance.
	 * @param objects the objects.
	 */
	public static void startParallel(Object... objects) {
		manageLifecycleParallel(SmartLifecycle::start, objects);
	}

	/**
	 * Starts the provided objects that are a {@link SmartLifecycle} instance.
	 * @param objects the objects.
	 */
	public static void stopParallel(Object... objects) {
		manageLifecycleParallel(SmartLifecycle::stop, objects);
	}

	public static boolean isRunning(Object object) {
		if (object instanceof SmartLifecycle) {
			return ((SmartLifecycle) object).isRunning();
		}
		return true;
	}

}
