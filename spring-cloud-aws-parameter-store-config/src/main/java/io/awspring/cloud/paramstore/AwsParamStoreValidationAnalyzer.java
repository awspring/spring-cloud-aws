package io.awspring.cloud.paramstore;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class AwsParamStoreValidationAnalyzer
		extends AbstractFailureAnalyzer<ValidationException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, ValidationException cause) {
		return new FailureAnalysis("Validation failed for field: " + cause.getField(),
				cause.getMessage(), cause);
	}
}
