/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.context.support.io.encryption;

import com.amazonaws.services.s3.AmazonS3;
import junit.framework.Assert;
import org.elasticspring.support.TestStackEnvironment;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alain Sahli
 * @since 1.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("SecretKeyEncryptionClientTest-context.xml")
public class SecretKeyEncryptionClientTest {

	private static final String S3_PREFIX = "s3://";

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Autowired
	private TestStackEnvironment testStackEnvironment;

	@SuppressWarnings("SpringJavaAutowiringInspection")
	@Autowired
	private ResourcePatternResolver resourceLoader;

	@SuppressWarnings("SpringJavaAutowiringInspection")
	@Autowired
	private AmazonS3 amazonS3;
	private final List<String> createdObjects = new ArrayList<String>();

	@Test
	public void testWriteEncryptedObjects() throws Exception {
		String bucketName = this.testStackEnvironment.getByLogicalId("EmptyBucket");
		String key = "test-encryption.txt";

		File file = this.temporaryFolder.newFile(key);
		String originalFileCheckSum = createDummyFile(file, 6);

		Resource resource = this.resourceLoader.getResource(S3_PREFIX + bucketName + "/" + key);

		int read;
		if (resource instanceof WritableResource) {
			WritableResource writableResource = (WritableResource) resource;
			OutputStream outputStream = writableResource.getOutputStream();
			FileInputStream fileInputStream = new FileInputStream(file);
			while ((read = fileInputStream.read()) != -1) {
				outputStream.write(read);
			}
			outputStream.close();
			this.createdObjects.add(key);
		}

		byte[] buffer = new byte[1024];
		MessageDigest md = MessageDigest.getInstance("MD5");
		InputStream downloadInputStream = resource.getInputStream();
		while ((read = downloadInputStream.read(buffer)) != -1) {
			md.update(buffer, 0, read);
		}
		downloadInputStream.close();

		String downloadedFileChecksum = DigestUtils.md5DigestAsHex(md.digest());

		Assert.assertEquals(originalFileCheckSum, downloadedFileChecksum);
	}

	private String createDummyFile(File file, int sizeInMB) throws NoSuchAlgorithmException, IOException {
		OutputStream outputStream = null;
		MessageDigest md = MessageDigest.getInstance("MD5");
		try {
			outputStream = new DigestOutputStream(new FileOutputStream(file), md);
			for (int i = 0; i < sizeInMB; i++) {
				for (int j = 0; j < (1024 * 1024); j++) {
					outputStream.write("c".getBytes("UTF-8"));
				}
			}
		} finally {
			if (outputStream != null) {
				outputStream.close();
			}
		}

		return DigestUtils.md5DigestAsHex(md.digest());
	}

	//Cleans up the bucket. Because if the bucket is not cleaned up, then the bucket will not be deleted after the test run.
	@After
	public void tearDown() {
		String bucketName = this.testStackEnvironment.getByLogicalId("EmptyBucket");
		for (String createdObject : this.createdObjects) {
			this.amazonS3.deleteObject(bucketName, createdObject);
		}

	}

}
