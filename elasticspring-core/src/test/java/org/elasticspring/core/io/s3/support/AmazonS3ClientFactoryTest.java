package org.elasticspring.core.io.s3.support;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import org.elasticspring.core.region.S3Region;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Alain Sahli
 */
public class AmazonS3ClientFactoryTest {

	@Test
	public void testCacheUnderHighConcurrency() throws InterruptedException {
		AWSCredentialsProvider awsCredentialsProviderMock = Mockito.mock(AWSCredentialsProvider.class);
		final AmazonS3ClientFactory factory = new AmazonS3ClientFactory(awsCredentialsProviderMock);

		int nThreads = 100;
		ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		final Set<AmazonS3> amazonS3Clients = new HashSet<AmazonS3>();

		for (int i = 0; i < nThreads; i++) {
			executorService.execute(new Runnable() {

				@Override
				public void run() {
					try {
						countDownLatch.await();
						amazonS3Clients.add(factory.getClientForRegion(S3Region.EU_WEST_1));
						amazonS3Clients.add(factory.getClientForRegion(S3Region.SA_EAST_1));
					} catch (InterruptedException e) {
						fail("Error awaiting latch");
					}
				}
			});
		}

		countDownLatch.countDown();
		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.SECONDS);
		assertEquals(2, amazonS3Clients.size());
	}

}
