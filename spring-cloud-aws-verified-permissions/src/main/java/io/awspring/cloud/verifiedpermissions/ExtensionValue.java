/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.verifiedpermissions;

import org.springframework.util.Assert;

/**
 * A Cedar extension value given in its string form, for when the Java type mapping is not wanted. The four extension
 * types require a Cedar 4 policy store.
 *
 * @param kind the extension type
 * @param value the string form, for example {@code 10.0.0.0/8} or {@code 1h30m}
 * @author Matej Nedic
 * @since 4.3.0
 */
public record ExtensionValue(Kind kind, String value) {

	public ExtensionValue {
		Assert.notNull(kind, "kind must not be null");
		Assert.hasText(value, "value must not be empty");
	}

	public static ExtensionValue ipaddr(String value) {
		return new ExtensionValue(Kind.IPADDR, value);
	}

	public static ExtensionValue decimal(String value) {
		return new ExtensionValue(Kind.DECIMAL, value);
	}

	public static ExtensionValue datetime(String value) {
		return new ExtensionValue(Kind.DATETIME, value);
	}

	public static ExtensionValue duration(String value) {
		return new ExtensionValue(Kind.DURATION, value);
	}

	public enum Kind {
		IPADDR, DECIMAL, DATETIME, DURATION
	}
}
