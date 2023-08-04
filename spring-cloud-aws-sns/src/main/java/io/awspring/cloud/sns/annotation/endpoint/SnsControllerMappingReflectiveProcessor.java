/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.sns.annotation.endpoint;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.annotation.ReflectiveProcessor;
import org.springframework.core.MethodParameter;

/**
 * Heavily inspired by Spring Frameworks ControllerMappingReflectiveProcessor.
 *
 * @author Matej Nedic
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @since 3.0.2
 */
public class SnsControllerMappingReflectiveProcessor implements ReflectiveProcessor {
	private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

	@Override
	public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
		if (element instanceof Class<?> type) {
			registerTypeHints(hints, type);
		}
		else if (element instanceof Method method) {
			registerMethodHints(hints, method);
		}
	}

	protected void registerTypeHints(ReflectionHints hints, Class<?> type) {
		hints.registerType(type);
	}

	protected void registerMethodHints(ReflectionHints hints, Method method) {
		hints.registerMethod(method, ExecutableMode.INVOKE);
		for (Parameter parameter : method.getParameters()) {
			registerParameterTypeHints(hints, MethodParameter.forParameter(parameter));
		}
		registerReturnTypeHints(hints, MethodParameter.forExecutable(method, -1));
	}

	protected void registerParameterTypeHints(ReflectionHints hints, MethodParameter methodParameter) {
		this.bindingRegistrar.registerReflectionHints(hints, methodParameter.getGenericParameterType());
	}

	protected void registerReturnTypeHints(ReflectionHints hints, MethodParameter returnTypeParameter) {
		this.bindingRegistrar.registerReflectionHints(hints, getEntityType(returnTypeParameter));
	}

	private Type getEntityType(MethodParameter parameter) {
		MethodParameter nestedParameter = parameter.nested();
		return (nestedParameter.getNestedParameterType() == nestedParameter.getParameterType() ? null
				: nestedParameter.getNestedParameterType());
	}
}
