package io.awspring.cloud.sqs.annotation;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.annotation.ReflectiveProcessor;
import org.springframework.core.MethodParameter;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

/**
 * Heavily inspired by Spring Frameworks ControllerMappingReflectiveProcessor.
 *
 * @author Matej Nedic
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @since 3.0.2
 */
public class SqsListenerReflectiveProcessor implements ReflectiveProcessor {

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
