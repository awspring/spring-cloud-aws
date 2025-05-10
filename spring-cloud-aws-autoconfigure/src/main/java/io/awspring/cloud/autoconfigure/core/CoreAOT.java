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
package io.awspring.cloud.autoconfigure.core;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.util.ClassUtils;

public class CoreAOT implements RuntimeHintsRegistrar {

	private static final String STS_WEB_IDENTITY_TOKEN_FILE_CREDENTIALS_PROVIDER = "software.amazon.awssdk.services.sts.auth.StsWebIdentityTokenFileCredentialsProvider";

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		if (ClassUtils.isPresent(STS_WEB_IDENTITY_TOKEN_FILE_CREDENTIALS_PROVIDER, classLoader)) {
			hints.reflection().registerType(TypeReference.of(STS_WEB_IDENTITY_TOKEN_FILE_CREDENTIALS_PROVIDER),
					hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS));
		}
	}
}
