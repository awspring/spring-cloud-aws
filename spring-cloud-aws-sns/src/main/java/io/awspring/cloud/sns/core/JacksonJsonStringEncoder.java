package io.awspring.cloud.sns.core;

public class JacksonJsonStringEncoder implements JsonStringEncoder {
	private final tools.jackson.core.io.JsonStringEncoder delegate = tools.jackson.core.io.JsonStringEncoder.getInstance();

	@Override
	public void quoteAsString(CharSequence input, StringBuilder output) {
		delegate.quoteAsString(input, output);
	}
}
