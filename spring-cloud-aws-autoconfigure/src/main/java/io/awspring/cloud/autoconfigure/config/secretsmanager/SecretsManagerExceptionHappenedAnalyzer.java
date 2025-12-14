package io.awspring.cloud.autoconfigure.config.secretsmanager;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class SecretsManagerExceptionHappenedAnalyzer extends AbstractFailureAnalyzer<AwsSecretsManagerPropertySourceNotFoundException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, AwsSecretsManagerPropertySourceNotFoundException cause) {
		return new FailureAnalysis("Could not import properties from AWS Secrets Manager. Exception happened while trying to load the keys: " + cause.getMessage(),
			"Depending on error message determine action course", cause);
	}
}
