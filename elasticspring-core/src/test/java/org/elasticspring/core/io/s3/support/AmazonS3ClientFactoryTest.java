package org.elasticspring.core.io.s3.support;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import org.elasticspring.core.io.s3.S3ServiceEndpoint;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

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
		final AmazonS3ClientFactory factory = getAmazonS3ClientFactory();

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
		AmazonS3ClientFactory factory = getAmazonS3ClientFactory();

		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(1024);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		factory.setKeyPairRef(keyPair);

		AmazonS3 amazonS3Client = factory.getClientForServiceEndpoint(S3ServiceEndpoint.US_STANDARD);
		Assert.assertTrue(amazonS3Client instanceof AmazonS3EncryptionClient);
	}

	@Test
	public void testInstantiationWithKeyPairResource() throws Exception {
		AmazonS3ClientFactory amazonS3ClientFactory = getAmazonS3ClientFactory();

		amazonS3ClientFactory.setPrivateKeyResource(new ClassPathResource("private-key.pem"));
		amazonS3ClientFactory.setPublicKeyResource(new ClassPathResource("public-key.pem"));
		AmazonS3 clientForServiceEndpoint = amazonS3ClientFactory.getClientForServiceEndpoint(S3ServiceEndpoint.US_STANDARD);
	}

	@Test
	public void testInstantiationWithSymmetricKeyRef() throws NoSuchAlgorithmException {
		AmazonS3ClientFactory factory = getAmazonS3ClientFactory();

		KeyGenerator keyGenerator = KeyGenerator.getInstance("DESede");
		SecretKey secretKey = keyGenerator.generateKey();
		factory.setSymmetricKeyRef(secretKey);

		AmazonS3 amazonS3Client = factory.getClientForServiceEndpoint(S3ServiceEndpoint.US_STANDARD);
		Assert.assertTrue(amazonS3Client instanceof AmazonS3EncryptionClient);
	}

	private AmazonS3ClientFactory getAmazonS3ClientFactory() {
		AWSCredentialsProvider awsCredentialsProviderMock = Mockito.mock(AWSCredentialsProvider.class);
		return new AmazonS3ClientFactory(awsCredentialsProviderMock);
	}
}
