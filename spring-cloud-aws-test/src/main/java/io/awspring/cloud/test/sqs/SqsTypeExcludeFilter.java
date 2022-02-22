/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.awspring.cloud.test.sqs;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.autoconfigure.filter.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * {@link TypeExcludeFilter} for {@link SqsTest @SqsTest}.
 *
 * @author Maciej Walkowiak
 * @since 2.4.0
 */
public class SqsTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<SqsTest> {

	private static final Class<?>[] NO_LISTENERS = {};

	private final Class<?>[] listeners;

	SqsTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
		this.listeners = getAnnotation().getValue("listeners", Class[].class).orElse(NO_LISTENERS);
	}

	@Override
	protected Set<Class<?>> getComponentIncludes() {
		return new LinkedHashSet<>(Arrays.asList(this.listeners));
	}

}
