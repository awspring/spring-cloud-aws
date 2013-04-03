package org.elasticspring.core.io.s3.support;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import org.elasticspring.core.io.s3.S3ServiceEndpoint;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
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
		final AmazonS3ClientFactory factory = getAmazonS3ClientWithCredentialsProviderFactory();

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
						amazonS3Clients.add(factory.getClientForServiceEndpoint(S3ServiceEndpoint.IRELAND));
						amazonS3Clients.add(factory.getClientForServiceEndpoint(S3ServiceEndpoint.SAO_PAULO));
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

	@Test
	public void testInstantiationWithKeyPairRef() throws NoSuchAlgorithmException {
		AmazonS3ClientFactory factory = getAmazonS3ClientWithCredentialsProviderFactory();

		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(1024);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		factory.setKeyPair(keyPair);

		AmazonS3 amazonS3Client = factory.getClientForServiceEndpoint(S3ServiceEndpoint.US_STANDARD);
		Assert.assertTrue(amazonS3Client instanceof AmazonS3EncryptionClient);
	}

	@Test
	public void testInstantiationWithSecretKeyRef() throws NoSuchAlgorithmException {
		AmazonS3ClientFactory factory = getAmazonS3ClientWithCredentialsProviderFactory();

		KeyGenerator keyGenerator = KeyGenerator.getInstance("DESede");
		SecretKey secretKey = keyGenerator.generateKey();
		factory.setSecretKey(secretKey);

		AmazonS3 amazonS3Client = factory.getClientForServiceEndpoint(S3ServiceEndpoint.US_STANDARD);
		Assert.assertTrue(amazonS3Client instanceof AmazonS3EncryptionClient);
	}

	@Test
	public void testInstantiationWithAnonymousFlagAndSecretKeyRef() throws Exception {
		AmazonS3ClientFactory factory = new AmazonS3ClientFactory();

		KeyGenerator keyGenerator = KeyGenerator.getInstance("DESede");
		SecretKey secretKey = keyGenerator.generateKey();
		factory.setSecretKey(secretKey);
		factory.setAnonymous(true);

		AmazonS3 amazonS3Client = factory.getClientForServiceEndpoint(S3ServiceEndpoint.US_STANDARD);
		Assert.assertTrue(amazonS3Client instanceof AmazonS3EncryptionClient);
	}

	@Test
	public void testInstantiationWithAnonymousFlagAndKeyPairRef() throws Exception {
		AmazonS3ClientFactory factory = new AmazonS3ClientFactory();

		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(1024);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		factory.setKeyPair(keyPair);
		factory.setAnonymous(true);

		AmazonS3 amazonS3Client = factory.getClientForServiceEndpoint(S3ServiceEndpoint.US_STANDARD);
		Assert.assertTrue(amazonS3Client instanceof AmazonS3EncryptionClient);
	}

	private AmazonS3ClientFactory getAmazonS3ClientWithCredentialsProviderFactory() {
		AWSCredentialsProvider awsCredentialsProviderMock = Mockito.mock(AWSCredentialsProvider.class);
		return new AmazonS3ClientFactory(awsCredentialsProviderMock);
	}

}
