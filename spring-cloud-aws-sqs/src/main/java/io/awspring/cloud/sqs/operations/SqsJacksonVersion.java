package io.awspring.cloud.sqs.operations;

/**
 * Enum used to define which default MessageConverter will be used when using {@link SqsTemplateBuilder} configureDefaultConverter.
 * @author Matej Nedic
 */
@Deprecated(forRemoval = true)
public enum SqsJacksonVersion {
	JACKSON_2,
	JACKSON_3
}
