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
package io.awspring.cloud.autoconfigure.config;

import io.awspring.cloud.autoconfigure.config.parameterstore.ParameterStorePropertySources;
import io.awspring.cloud.autoconfigure.config.secretsmanager.SecretsManagerPropertySources;
import io.awspring.cloud.parameterstore.ParameterStorePropertySource;
import io.awspring.cloud.secretsmanager.SecretsManagerPropertySource;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.util.ClassUtils;

public class ConfigStoreRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		if (ClassUtils.isPresent("io.awspring.cloud.parameterstore.ParameterStorePropertySource", classLoader)) {
			hints.reflection().registerType(TypeReference.of(ParameterStorePropertySources.class),
					hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS));
			hints.reflection().registerType(TypeReference.of(ParameterStorePropertySource.class),
					hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS));
		}

		if (ClassUtils.isPresent("io.awspring.cloud.secretsmanager.SecretsManagerPropertySource", classLoader)) {
			hints.reflection().registerType(TypeReference.of(SecretsManagerPropertySources.class),
					hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS));

			hints.reflection().registerType(TypeReference.of(SecretsManagerPropertySource.class),
					hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS));
		}

	}
}
