/*
 * Copyright 2013-2024 the original author or authors.
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
package io.awspring.cloud.docker.compose;

import java.util.Map;

/**
 * LocalStack environment details.
 *
 * @author Dominik Kovács
 * @since 3.2.0
 */
class LocalStackEnvironment {

	private final String region;

	private final String accessKey;

	private final String secretKey;

	LocalStackEnvironment(Map<String, String> env) {
		this.region = env.get("AWS_DEFAULT_REGION");
		this.accessKey = env.get("AWS_ACCESS_KEY_ID");
		this.secretKey = env.get("AWS_SECRET_ACCESS_KEY");
	}

	String getRegion() {
		return this.region;
	}

	String getAccessKey() {
		return this.accessKey;
	}

	String getSecretKey() {
		return this.secretKey;
	}
}
