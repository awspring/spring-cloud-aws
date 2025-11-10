package io.awspring.cloud.sns.core;

import io.awspring.cloud.core.support.JacksonPresent;

public interface JsonStringEncoder {
	static JsonStringEncoder create() {
		if (JacksonPresent.isJackson3Present()) {
			return new JacksonJsonStringEncoder();
		} else if (JacksonPresent.isJackson2Present()) {
			return new Jackson2JsonStringEncoder();
		} else {
			throw new IllegalStateException("JsonStringEncoder requires a Jackson 2 or Jackson 3 library on the classpath");
		}
	}
	void quoteAsString(CharSequence input, StringBuilder output);
}
