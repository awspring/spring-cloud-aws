/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.messaging.listener;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteMessageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callback executed on SQS message deletion.
 *
 * @author Mete Alpaslan Katırcıoğlu
 */
class DeleteMessageHandler implements AsyncHandler<DeleteMessageRequest, DeleteMessageResult> {

	private static final Logger logger = LoggerFactory.getLogger(DeleteMessageHandler.class);

	private final String receiptHandle;

	DeleteMessageHandler(String receiptHandle) {
		this.receiptHandle = receiptHandle;
	}

	@Override
	public void onError(Exception exception) {
		logger.warn("An exception occurred while deleting '{}' receiptHandle", receiptHandle, exception);
	}

	@Override
	public void onSuccess(DeleteMessageRequest request, DeleteMessageResult deleteMessageResult) {
		logger.trace("'{}' receiptHandle is deleted successfully", request.getReceiptHandle());
	}

}
