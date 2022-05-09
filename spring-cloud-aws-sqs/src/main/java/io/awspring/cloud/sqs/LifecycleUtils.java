package io.awspring.cloud.sqs;

import org.springframework.context.SmartLifecycle;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * Utility methods to handle {@link SmartLifecycle} hooks.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class LifecycleUtils {

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
				((Collection<?>) object).parallelStream()
					.forEach(innerObject -> manageLifecycle(action, innerObject));
			}
		});
	}

	/**
	 * Starts the provided objects that are a {@link SmartLifecycle} instance.
	 * @param objects the objects.
	 */
	public static void start(Object... objects) {
		manageLifecycle(SmartLifecycle::start, objects);
	}

	/**
	 * Starts the provided objects that are a {@link SmartLifecycle} instance.
	 * @param objects the objects.
	 */
	public static void stop(Object... objects) {
		manageLifecycle(SmartLifecycle::stop, objects);
	}

	public static boolean isRunning(Object object) {
		if (object instanceof SmartLifecycle) {
			return ((SmartLifecycle) object).isRunning();
		}
		return true;
	}

}
