package io.awspring.cloud.core.config;

import org.springframework.core.env.EnumerablePropertySource;

public abstract class AwsPropertySource<K extends AwsPropertySource, T> extends EnumerablePropertySource<T>  {
	public AwsPropertySource(String name, T source) {
		super(name, source);
	}

	abstract public void init();
	abstract public K copy();
}
