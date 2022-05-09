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
package io.awspring.cloud.sqs;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Class with convenient expression resolving methods.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class ExpressionResolvingHelper implements BeanFactoryAware {

	private static final String THE_LEFT = "The [";

	private static final String RESOLVED_TO_LEFT = "Resolved to [";

	private static final String RIGHT_FOR_LEFT = "] for [";

	private BeanFactory beanFactory;

	private BeanExpressionResolver resolver = new StandardBeanExpressionResolver();

	private BeanExpressionContext expressionContext;

	/**
	 * Resolves a given value as {@link String}.
	 * @param value the value to be resolved.
	 * @param attribute the attribute name to be used in case of an exception.
	 * @return the resolved {@link String}
	 */
	@Nullable
	public String asString(String value, String attribute) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		Object resolved = resolveExpression(value);
		if (resolved instanceof String) {
			return (String) resolved;
		}
		else if (resolved != null) {
			throw new IllegalStateException(THE_LEFT + attribute + "] must resolve to a String. " + RESOLVED_TO_LEFT
					+ resolved.getClass() + RIGHT_FOR_LEFT + value + "]");
		}
		return null;
	}

	/**
	 * Resolves a given value as {@link Integer}.
	 * @param value the value to be resolved.
	 * @param attribute the attribute name to be used in case of an exception.
	 * @return the resolved {@link Integer}
	 */
	@Nullable
	public Integer asInteger(String value, String attribute) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		Object resolved = resolveExpression(value);
		if (resolved instanceof Integer) {
			return (Integer) resolved;
		}
		else if (resolved instanceof String) {
			return Integer.parseInt((String) resolved);
		}
		else if (resolved != null) {
			throw new IllegalStateException(THE_LEFT + attribute + "] must resolve to Integer. " + RESOLVED_TO_LEFT
					+ resolved.getClass() + RIGHT_FOR_LEFT + value + "]");
		}
		return null;
	}

	@Nullable
	public Boolean asBoolean(String value, String attribute) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		Object resolved = resolveExpression(value);
		if (resolved instanceof Boolean) {
			return (Boolean) resolved;
		}
		else if (resolved instanceof String) {
			return Boolean.parseBoolean((String) resolved);
		}
		else if (resolved != null) {
			throw new IllegalStateException(THE_LEFT + attribute + "] must resolve to Boolean. " + RESOLVED_TO_LEFT
					+ resolved.getClass() + RIGHT_FOR_LEFT + value + "]");
		}
		return null;
	}

	private Object resolveExpression(String value) {
		return this.resolver.evaluate(resolve(value), this.expressionContext);
	}

	private String resolve(String value) {
		if (this.beanFactory instanceof ConfigurableBeanFactory) {
			return ((ConfigurableBeanFactory) this.beanFactory).resolveEmbeddedValue(value);
		}
		return value;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			this.resolver = ((ConfigurableListableBeanFactory) beanFactory).getBeanExpressionResolver();
			this.expressionContext = new BeanExpressionContext((ConfigurableListableBeanFactory) beanFactory, null);
		}
	}

}
