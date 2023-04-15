package io.awspring.cloud.core;

import io.awspring.cloud.core.SpringCloudClientConfiguration.SpringCloudClientConfigurationHints;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringCloudClientConfigurationHintsTests {

	private final SpringCloudClientConfigurationHints springCloudClientConfigurationHints = new SpringCloudClientConfigurationHints();

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		this.springCloudClientConfigurationHints.registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.resource().forResource("io/awspring/cloud/core/SpringCloudClientConfiguration.properties")).accepts(runtimeHints);
	}
}
