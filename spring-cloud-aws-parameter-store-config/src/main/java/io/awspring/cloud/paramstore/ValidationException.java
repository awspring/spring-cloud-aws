package io.awspring.cloud.paramstore;

public class ValidationException extends RuntimeException{
	private String field;

	public ValidationException(String message, String field) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}
}
