package io.awspring.cloud.s3;

import edu.colorado.cires.cmg.s3out.S3ClientMultipartUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.s3.S3Client;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link S3ProtocolResolverTests}.
 *
 * @author Maciej Walkowiak
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
// context must be cleaned up before each method to make sure that for each use case
// protocol resolver is registered before resource is requested
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class S3ProtocolResolverTests {

	@Autowired
	@Qualifier("configurationLoadedResource")
	private Resource resource;

	@Value("s3://foo/bar.txt")
	private Resource fieldResource;

	@Autowired
	private ResourceLoader resourceLoader;

	@Test
	void configurationClassAnnotatedResourceResolvesToS3Resource() throws Exception {
		assertThat(((Advised) resource).getTargetSource().getTarget()).isInstanceOf(S3Resource.class);
	}

	@Test
	void valueAnnotatedResourceResolvesToS3Resource() {
		assertThat(fieldResource).isInstanceOf(S3Resource.class);
	}

	@Test
	void resourceLoadedResourceIsS3Resource() {
		assertThat(resourceLoader.getResource("s3://foo/bar.txt")).isInstanceOf(S3Resource.class);
	}

	@Configuration
	@Import(S3ProtocolResolver.class)
	static class Config {

		@Value("s3://foo/bar.txt")
		@Lazy
		private Resource resource;

		@Bean
		public Resource configurationLoadedResource() {
			return resource;
		}

		@Bean
		S3Client s3Client() {
			return mock(S3Client.class);
		}

		@Bean
		S3ClientMultipartUpload s3ClientMultipartUpload() {
			return mock(S3ClientMultipartUpload.class);
		}

	}

}
