package org.elasticspring.core.env.ec2;

import org.elasticspring.support.TestStackInstanceIdService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AmazonEC2InstanceIdProviderAwsTest {

	private TestStackInstanceIdService testStackInstanceIdService;

	@Before
	public void mockInstanceIdMetadataService() throws Exception {
		this.testStackInstanceIdService = TestStackInstanceIdService.fromInstanceId("i-abcdefg");
		this.testStackInstanceIdService.afterPropertiesSet();
	}

	@After
	public void resetInstanceIdMetadataService() throws Exception {
		this.testStackInstanceIdService.destroy();
	}

	@Test
	public void getCurrentInstanceId_instanceIdAvailableViaMetadataService_returnsInstanceIdFromMetadataService() throws IOException {
		// Arrange
		AmazonEC2InstanceIdProvider amazonEC2InstanceIdProvider = new AmazonEC2InstanceIdProvider();

		// Act
		String currentInstanceId = amazonEC2InstanceIdProvider.getCurrentInstanceId();

		// Assert
		assertThat(currentInstanceId, is("i-abcdefg"));
	}

}
