package io.awspring.cloud.autoconfigure.config.appconfig;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class AppConfigMissingKeysFailureAnalyzer extends AbstractFailureAnalyzer<AppConfigKeysMissingException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, AppConfigKeysMissingException cause) {
		return new FailureAnalysis("Could not import properties from AWS App Config: " + cause.getMessage(),
			"Consider providing keys, for example `spring.config.import=aws-appconfig:/config#app#prod`", cause);
	}

}
