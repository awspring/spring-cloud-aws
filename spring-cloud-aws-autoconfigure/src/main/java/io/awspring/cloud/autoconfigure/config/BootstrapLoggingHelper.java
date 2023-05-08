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
package io.awspring.cloud.autoconfigure.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Utility class for setting up logging in objects registered in {@link BootstrapRegistry}.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
public final class BootstrapLoggingHelper {

	private BootstrapLoggingHelper() {
	}

	public static void reconfigureLoggers(DeferredLogFactory logFactory, String... classes) {
		// loggers in these classes must be static non-final
		List<Class<?>> loggers = new ArrayList<>();
		for (String clazz : classes) {
			if (ClassUtils.isPresent(clazz, null)) {
				try {
					loggers.add(Class.forName(clazz));
				}
				catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		}

		loggers.forEach(it -> reconfigureLogger(it, logFactory));
	}

	private static void reconfigureLogger(Class<?> type, DeferredLogFactory logFactory) {
		ReflectionUtils.doWithFields(type, field -> {

			field.setAccessible(true);
			field.set(null, logFactory.getLog(type));

		}, BootstrapLoggingHelper::isUpdateableLogField);
	}

	private static boolean isUpdateableLogField(Field field) {
		return !Modifier.isFinal(field.getModifiers()) && field.getType().isAssignableFrom(Log.class);
	}

}
