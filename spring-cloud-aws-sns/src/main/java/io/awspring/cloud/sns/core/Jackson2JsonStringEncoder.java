package io.awspring.cloud.sns.core;

@Deprecated
public class Jackson2JsonStringEncoder implements JsonStringEncoder {
	private final com.fasterxml.jackson.core.io.JsonStringEncoder delegate = com.fasterxml.jackson.core.io.JsonStringEncoder.getInstance();

	@Override
	public void quoteAsString(CharSequence input, StringBuilder output) {
		delegate.quoteAsString(input, output);
	}
}
