package io.awspring.cloud.sqs.operations;

/**
 * The strategy to use when handling a send batch operation that has at least one failed message.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public enum SendBatchFailureStrategy {

	/**
	 * Throw a {@link SendBatchOperationFailedException} with a {@link SendResult.Batch} object.
	 */
	THROW_EXCEPTION,

	/**
	 * Do not throw an exception and return the {@link SendResult.Batch} object directly.
	 */
	RETURN_RESULT

}
