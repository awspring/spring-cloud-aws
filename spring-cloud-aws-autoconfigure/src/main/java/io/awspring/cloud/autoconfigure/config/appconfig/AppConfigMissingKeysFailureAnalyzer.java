package io.awspring.cloud.autoconfigure.config.appconfig;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of an AppConfig configuration failure caused by not providing an AppConfig
 * key to `spring.config.import` property.
 *
 * @author Matej Nedic
 * @since 4.1.0
 */
public class AppConfigMissingKeysFailureAnalyzer extends AbstractFailureAnalyzer<AppConfigKeysMissingException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, AppConfigKeysMissingException cause) {
		return new FailureAnalysis("Could not import properties from AWS App Config: " + cause.getMessage(),
			"Consider providing keys, for example `spring.config.import=aws-appconfig:/config#app#prod`", cause);
	}

}
