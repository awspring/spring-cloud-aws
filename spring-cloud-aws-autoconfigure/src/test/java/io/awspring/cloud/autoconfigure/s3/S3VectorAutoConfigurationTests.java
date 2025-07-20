package io.awspring.cloud.autoconfigure.s3;

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.s3.properties.S3Properties;
import io.awspring.cloud.s3.S3OutputStreamProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3vectors.S3VectorsClient;
import software.amazon.encryption.s3.S3EncryptionClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link S3AutoConfiguration} class
 *
 * @author Matej Nedic
 */
public class S3VectorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
		.withConfiguration(AutoConfigurations.of(AwsAutoConfiguration.class, RegionProviderAutoConfiguration.class,
			CredentialsProviderAutoConfiguration.class, S3VectorClientAutoConfiguration.class));


	@Test
	void createsS3VectorClientBean() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(S3VectorsClient.class);
		});
	}

	@Test
	void s3VectorClientAutoConfigurationIsDisabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.s3.vector.enabled:false").run(context -> {
			assertThat(context).doesNotHaveBean(S3VectorsClient.class);
		});
	}
}
