/*
 * Copyright 2013-2024 the original author or authors.
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

import io.micrometer.context.ContextSnapshot;
import io.micrometer.observation.ObservationRegistry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

/**
 * @author Mariusz Sondecki
 */
public final class ContextScopeManager {

	private static final Logger logger = LoggerFactory.getLogger(ContextScopeManager.class);

	private final ContextSnapshot currentContextSnapshot;

	@Nullable
	private final ContextSnapshot previousContextSnapshot;

	private final ObservationRegistry observationRegistry;

	@Nullable
	private ContextSnapshot.Scope currentScope;

	public ContextScopeManager(ContextSnapshot currentContextSnapshot,
			@Nullable ContextSnapshot previousContextSnapshot, ObservationRegistry observationRegistry) {
		this.currentContextSnapshot = currentContextSnapshot;
		this.previousContextSnapshot = previousContextSnapshot;
		this.observationRegistry = observationRegistry;

		logNoOpObservationRegistry();
	}

	public void restoreScope() {
		if (observationRegistry.isNoop()) {
			return;
		}
		closeCurrentScope(); // Ensure current scope is closed
		if (observationRegistry.getCurrentObservationScope() == null && previousContextSnapshot != null) {
			restorePreviousScope();
		}
		else {
			logger.trace(
					"Current observation scope is active or previous scope is not available; not restoring previous scope");
		}
	}

	// @formatter:off
	public <T, U> CompletableFuture<U> manageContextWhileComposing(CompletableFuture<T> future,
														 Function<? super T, ? extends CompletableFuture<U>> fn) {
		if (observationRegistry.isNoop()){
			return future.thenCompose(fn);
		}
		return future
			.whenComplete((t, throwable) -> closeCurrentScope())
			.thenCompose(fn)
			.whenComplete((u, throwable) -> openCurrentScope());
	}
	// @formatter:on

	private void restorePreviousScope() {
		logger.trace("Restoring previous scope");
		previousContextSnapshot.setThreadLocals();
		logger.debug("Previous scope restored successfully");
	}

	private void openCurrentScope() {
		logger.trace("Opening scope");
		currentScope = currentContextSnapshot.setThreadLocals();
		logger.debug("Scope opened successfully");
	}

	private void closeCurrentScope() {
		if (currentScope != null && observationRegistry.getCurrentObservationScope() != null) {
			try {
				logger.trace("Closing scope");
				currentScope.close();
				currentScope = null;
				logger.debug("Scope closed successfully");
			}
			catch (Exception e) {
				logger.error("Failed to close scope", e);
			}
		}
		else {
			logger.trace("No scope to close");
		}
	}

	private void logNoOpObservationRegistry() {
		if (observationRegistry.isNoop()) {
			logger.trace("ObservationRegistry is in No-Op mode");
		}
	}
}
