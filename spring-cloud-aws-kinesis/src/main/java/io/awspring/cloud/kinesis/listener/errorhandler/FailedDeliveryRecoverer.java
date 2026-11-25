/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.kinesis.listener.errorhandler;

/**
 * Decides what happens to a {@link FailedDelivery} once {@link BackOffErrorHandler} has exhausted its retries, or
 * immediately when the delivery is not {@link FailedDelivery#isRetryable() retryable}.
 * <p>
 * Returning normally resolves the failure, for example after the record was written to a dead-letter stream, and
 * processing continues past it. Throwing leaves it unresolved and halts the shard, see {@link ErrorHandler}.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
@FunctionalInterface
public interface FailedDeliveryRecoverer {

	/**
	 * Recover the failed delivery.
	 * @param failedDelivery the failed delivery.
	 * @param lastFailure the failure of the last attempt: the original failure, or the exception thrown by the last
	 *     {@link FailedDelivery#retry()}.
	 * @throws Exception to leave the failure unresolved and halt the shard.
	 */
	void recover(FailedDelivery failedDelivery, Throwable lastFailure) throws Exception;

}
