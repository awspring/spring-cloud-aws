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

import io.awspring.cloud.kinesis.listener.ListenerExecutionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link ErrorHandler}: logs the failure and leaves it unresolved, so the shard is halted and the records after
 * the last checkpoint are redelivered once the lease is acquired again. No record is skipped, at the cost of the shard
 * not progressing until then. Provide a custom {@link ErrorHandler} to retry or dead-letter instead.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public class LoggingErrorHandler implements ErrorHandler {

	private static final Logger logger = LoggerFactory.getLogger(LoggingErrorHandler.class);

	@Override
	public void handle(FailedDelivery failedDelivery) throws Exception {
		logger.error(
				"Error processing Kinesis records of stream {} shard {}; halting the shard until its lease "
						+ "is reacquired",
				failedDelivery.getStreamName(), failedDelivery.getShardId(), failedDelivery.getCause());
		Throwable cause = failedDelivery.getCause();
		if (cause instanceof Exception exception) {
			throw exception;
		}
		if (cause instanceof Error error) {
			throw error;
		}
		throw new ListenerExecutionFailedException("Kinesis listener failed", cause);
	}

}
