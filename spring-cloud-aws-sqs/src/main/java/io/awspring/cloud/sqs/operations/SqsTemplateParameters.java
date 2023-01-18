package io.awspring.cloud.sqs.operations;

/**
 * SQS parameters added to {@link SendResult} objects as additional information.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsTemplateParameters {

	public static final String SEQUENCE_NUMBER_PARAMETER_NAME = "sequenceNumber";

	public static final String SENDER_FAULT_PARAMETER_NAME = "senderFault";

	public static final String CODE_PARAMETER_NAME = "code";

}
