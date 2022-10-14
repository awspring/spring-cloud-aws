package io.awspring.cloud.core.config;

import org.springframework.core.env.EnumerablePropertySource;

/**
 * Base class for all AWS loaded property sources.
 *
 * @param <K> - self
 * @param <T> - the source type
 */
public abstract class AwsPropertySource<K extends AwsPropertySource, T> extends EnumerablePropertySource<T> {
	public AwsPropertySource(String name, T source) {
		super(name, source);
	}

	/**
	 * Initializes & fetches properties.
	 */
	abstract public void init();

	/**
	 * Creates a non-initialized copy of the property source.
	 *
	 * @return a property source
	 */
	abstract public K copy();
}
