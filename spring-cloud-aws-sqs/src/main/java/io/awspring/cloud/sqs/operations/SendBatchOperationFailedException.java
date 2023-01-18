package io.awspring.cloud.sqs.operations;

import org.springframework.lang.Nullable;

/**
 * Exception representing a partial or complete failure in sending a batch of messages to an endpoint.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SendBatchOperationFailedException extends MessagingOperationFailedException {

	private final SendResult.Batch<?> sendBatchResult;

	/**
	 * Create an instance with the provided arguments.
	 * @param msg the error message.
	 * @param endpoint the endpoint to which the messages were sent to.
	 * @param sendBatchResult the detailed result of the batch send attempt..
	 */
	public SendBatchOperationFailedException(String msg, String endpoint, SendResult.Batch<?> sendBatchResult) {
		this(msg, endpoint, sendBatchResult, null);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param msg the error message.
	 * @param endpoint the endpoint to which the messages were sent to.
	 * @param sendBatchResult the detailed result of the send message.
	 * @param cause the exception cause.
	 */
	public SendBatchOperationFailedException(String msg, String endpoint, SendResult.Batch<?> sendBatchResult,
											 @Nullable Throwable cause) {
		super(msg, endpoint, sendBatchResult.failed().stream().map(SendResult.Failed::message).toList(), cause);
		this.sendBatchResult = sendBatchResult;
	}

	/**
	 * Get the detailed result of the batch send attempt.
	 * @return the result.
	 */
	public SendResult.Batch<?> getSendBatchResult() {
		return this.sendBatchResult;
	}

	/**
	 * Get the detailed result of the batch send attempt,
	 * casting the result to the provided payload type.
	 * @return the result.
	 */
	@SuppressWarnings("unchecked")
	public <T> SendResult.Batch<T> getSendBatchResult(Class<T> payloadClass) {
		return (SendResult.Batch<T>) this.sendBatchResult;
	}

}
