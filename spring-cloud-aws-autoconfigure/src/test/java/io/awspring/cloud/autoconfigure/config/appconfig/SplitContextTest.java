package io.awspring.cloud.autoconfigure.config.appconfig;

import io.awspring.cloud.appconfig.RequestContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SplitContextTest {

	@Test
	void splitContextCorrect() {
		RequestContext requestContext = AppConfigDataLocationResolver.splitContext("spring_cloud_aws_cool_application___testClient10___test");
		assertThat(requestContext.getContext()).isEqualTo("spring_cloud_aws_cool_application___testClient10___test");
		assertThat(requestContext.getApplicationIdentifier()).isEqualTo("spring_cloud_aws_cool_application");
		assertThat(requestContext.getConfigurationProfileIdentifier()).isEqualTo("testClient10");
		assertThat(requestContext.getEnvironmentIdentifier()).isEqualTo("test");
	}
}
