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
package io.awspring.cloud.sqs;

import java.lang.reflect.Method;
import org.jspecify.annotations.Nullable;

/**
 * Utility class for virtual thread detection. Uses reflection to access {@code Thread.isVirtual()} (JDK 21+) while
 * maintaining compatibility with JDK 17.
 *
 * @author Tomaz Fernandes
 * @since 4.1.0
 * @see MessageExecutionThread
 */
public class VirtualThreadUtils {

	@Nullable
	private static final Method IS_VIRTUAL;

	static {
		Method m = null;
		try {
			m = Thread.class.getMethod("isVirtual");
		}
		catch (NoSuchMethodException e) {
			// JDK < 21
		}
		IS_VIRTUAL = m;
	}

	private VirtualThreadUtils() {
	}

	/**
	 * Check whether the given thread is a virtual thread.
	 * @param thread the thread to check.
	 * @return {@code true} if the thread is virtual, {@code false} if not or if running on JDK &lt; 21.
	 */
	public static boolean isVirtual(Thread thread) {
		if (IS_VIRTUAL == null) {
			return false;
		}
		try {
			return (boolean) IS_VIRTUAL.invoke(thread);
		}
		catch (Exception e) {
			return false;
		}
	}

}