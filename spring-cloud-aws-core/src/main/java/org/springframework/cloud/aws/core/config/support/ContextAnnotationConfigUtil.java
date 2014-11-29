/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.core.config.support;

import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * Utility class for configuration classes utility methods (e.g. resolving String values)
 *
 * @author Agim Emruli
 */
public final class ContextAnnotationConfigUtil {

	private ContextAnnotationConfigUtil() {
		//Static utility class
	}

	/**
	 * Resolves the expression values like <code>#{foo}</code> and placeholders like <code>${foo}</code> into the concrete
	 * String value. Useful for classes that need to resolve those String (e.g. config annotation classes)
	 *
	 * @param configurableBeanFactory
	 * 		- the {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} used
	 * 		to resolve the expressions or placeholders
	 * @param defaultValue
	 * 		- the defaultValue that will be resolved or returned in case it can not be resolved
	 * @return the resolved value or defaultValue (if it can not be resolved)
	 */
	public static String resolveStringValue(ConfigurableBeanFactory configurableBeanFactory, String defaultValue) {
		if (configurableBeanFactory == null) {
			return defaultValue;
		}
		String placeholdersResolved = configurableBeanFactory.resolveEmbeddedValue(defaultValue);
		BeanExpressionResolver exprResolver = configurableBeanFactory.getBeanExpressionResolver();
		if (exprResolver == null) {
			return defaultValue;
		}
		Object result = exprResolver.evaluate(placeholdersResolved, new BeanExpressionContext(configurableBeanFactory, null));
		return result != null ? result.toString() : defaultValue;
	}
}
