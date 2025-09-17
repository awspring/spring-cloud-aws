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
package io.awspring.cloud.sqs.listener.errorhandler;

import java.util.Random;
import java.util.function.Supplier;
import org.springframework.util.Assert;

/**
 * Strategy interface for applying jitter to exponential backoff calculations.
 *
 * @author Bruno Garcia
 * @author Rafael Pavarini
 * @author Tomaz Fernandes
 */
public interface Jitter {

	/**
	 * Apply jitter to the calculated timeout value.
	 *
	 * @param context the jitter context containing timeout and random supplier
	 * @return the timeout value with jitter applied
	 */
	int applyJitter(Context context);

	/**
	 * Context for jitter calculations.
	 */
	class Context {
		private final int timeout;
		private final Supplier<Random> randomSupplier;

		/**
		 * Create a new Context instance.
		 *
		 * @param timeout the timeout value (must be >= 1)
		 * @param randomSupplier the random supplier (must not be null)
		 */
		public Context(int timeout, Supplier<Random> randomSupplier) {
			Assert.isTrue(timeout >= 1, () -> "Timeout must be >= 1, but was " + timeout);
			Assert.notNull(randomSupplier, "Random supplier cannot be null");
			this.timeout = timeout;
			this.randomSupplier = randomSupplier;
		}

		public int getTimeout() {
			return timeout;
		}

		public Supplier<Random> getRandomSupplier() {
			return randomSupplier;
		}
	}

	/**
	 * No jitter strategy - returns the original timeout value. The original timeout value is expected to be valid
	 * (greater than 0) as it's validated in the builder.
	 */
	Jitter NONE = Context::getTimeout;

	/**
	 * Full jitter strategy - returns a random value between 0 and the original timeout. Ensures the result is at least
	 * 1 to avoid invalid timeout values.
	 */
	Jitter FULL = context -> Math.max(1, context.getRandomSupplier().get().nextInt(context.getTimeout() + 1));

	/**
	 * Half jitter strategy - returns a value uniformly between ceil(timeout/2) and timeout. Ensures the result is at
	 * least 1 to avoid invalid timeout values.
	 */
	Jitter HALF = context -> {
		int timeout = context.getTimeout();
		int lowerBound = Math.max(1, (int) Math.ceil(timeout / 2.0));
		return context.getRandomSupplier().get().nextInt(lowerBound, timeout + 1);
	};

}
