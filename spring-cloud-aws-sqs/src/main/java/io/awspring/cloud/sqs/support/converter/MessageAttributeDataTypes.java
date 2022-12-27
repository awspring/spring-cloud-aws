package io.awspring.cloud.sqs.support.converter;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public final class MessageAttributeDataTypes {

	private MessageAttributeDataTypes() {
		// Avoid instantiation
	}

	/**
	 * Binary message attribute data type.
	 */
	public static final String BINARY = "Binary";

	/**
	 * Number message attribute data type.
	 */
	public static final String NUMBER = "Number";

	/**
	 * String message attribute data type.
	 */
	public static final String STRING = "String";

	/**
	 * String.Array message attribute data type.
	 */
	public static final String STRING_ARRAY = "String.Array";

}
