package io.awspring.cloud.v3.it;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SES;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@IntegrationTest
@Testcontainers
public class LocalStackIntegrationTest {

	@Container
	private static LocalStackContainer localStackContainer = new LocalStackContainer(DockerImageName
		.parse("localstack/localstack"))
		.withServices(S3, SQS, SNS, SES)
		.withReuse(true);

	@DynamicPropertySource
	public void setupBoot(DynamicPropertyRegistry registry) {
		registry.add("spring.cloud.aws.s3.endpoint", () -> localStackContainer.getEndpointOverride(S3));
		registry.add("spring.cloud.aws.s3.region", () -> localStackContainer.getRegion());
		registry.add("spring.cloud.aws.sqs.endpoint", () -> localStackContainer.getEndpointOverride(SQS));
		registry.add("spring.cloud.aws.sqs.region", () -> localStackContainer.getRegion());
		registry.add("spring.cloud.aws.sns.endpoint", () -> localStackContainer.getEndpointOverride(SNS));
		registry.add("spring.cloud.aws.sns.region", () -> localStackContainer.getRegion());
		registry.add("spring.cloud.aws.ses.endpoint", () -> localStackContainer.getEndpointOverride(SES));
		registry.add("spring.cloud.aws.ses.region", () -> localStackContainer.getRegion());
		registry.add("spring.cloud.aws.ses.region", () -> localStackContainer.getRegion());
	}
}
