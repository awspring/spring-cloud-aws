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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utilities to handle exceptions.
 *
 * @author Tomaz Fernandes
 * @since 3.4
 */
public class ExceptionUtils {

	/**
	 * Unwraps the original exception until an exception not in the provided exceptions is found.
	 *
	 * @param original the exception to unwrap.
	 * @param exceptionsToUnwrap the exceptions from which the original exception should be unwrapped from.
	 * @return the unwrapped exception.
	 */
	@SafeVarargs
	public static Throwable unwrapException(Throwable original, Class<? extends Throwable>... exceptionsToUnwrap) {
		Set<Class<? extends Throwable>> exceptions = Arrays.stream(exceptionsToUnwrap).collect(Collectors.toSet());
		Throwable unwrapped = original;
		while (exceptions.contains(unwrapped.getClass()) && unwrapped.getCause() != null) {
			unwrapped = unwrapped.getCause();
		}
		return unwrapped;
	}

}
