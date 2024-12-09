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
package io.awspring.cloud.autoconfigure.config.s3;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of a S3 configuration failure caused by not providing a S3
 * key to `spring.config.import` property.
 *
 * @author Kunal Varpe
 * @since 3.3.0
 */
public class S3MissingKeysFailureAnalyzer extends AbstractFailureAnalyzer<S3KeysMissingException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, S3KeysMissingException cause) {
		return new FailureAnalysis("Could not import properties from AWS S3: " + cause.getMessage(),
				"Consider providing keys, for example `spring.config.import=aws-s3:bucket/application.properties`",
				cause);
	}

}
