package io.awspring.cloud.core.support;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ClassUtils;

/**
 * The utility to check if Jackson JSON processor is present in the classpath.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Soby Chacko
 *
 * @since 4.0
 */
public final class JacksonPresent {

	private static final @Nullable ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

	private static final boolean jackson2Present =
		ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
			ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);

	private static final boolean jackson3Present =
		ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", classLoader) &&
			ClassUtils.isPresent("tools.jackson.core.JsonGenerator", classLoader);

	public static boolean isJackson2Present() {
		return jackson2Present;
	}

	public static boolean isJackson3Present() {
		return jackson3Present;
	}

	private JacksonPresent() {
	}

}
