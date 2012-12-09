/*
 *
 *  * Copyright 2010-2012 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.elasticspring.messaging.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class StringMessage implements Message<String> {

	private final String payload;
	private final Map<String, String> attributes;

	public StringMessage(String payload) {
		this(payload, Collections.<String, String>emptyMap());
	}

	public StringMessage(String payload, Map<String, String> attributes) {
		this.payload = payload;
		this.attributes = attributes;
	}

	public Map<String, String> getAttributes() {
		return new HashMap<String, String>(this.attributes);
	}

	public String getPayload() {
		return this.payload;
	}

	@SuppressWarnings("MagicCharacter")
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("StringMessage");
		sb.append("{payload='").append(this.payload).append('\'');
		sb.append(", attributes=").append(this.attributes);
		sb.append('}');
		return sb.toString();
	}
}