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
package io.awspring.cloud.autoconfigure.config.parameterstore;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of a Parameter Store configuration failure caused by not
 * providing a Parameter Store key to `spring.config.import` property.
 *
 * @author Maciej Walkowiak
 * @since 3.0.0
 */
public class ParameterStoreMissingKeysFailureAnalyzer
		extends AbstractFailureAnalyzer<ParameterStoreKeysMissingException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, ParameterStoreKeysMissingException cause) {
		return new FailureAnalysis("Could not import properties from AWS Parameter Store: " + cause.getMessage(),
				"Consider providing keys, for example `spring.config.import=aws-parameterstore:/config/app`", cause);
	}

}
