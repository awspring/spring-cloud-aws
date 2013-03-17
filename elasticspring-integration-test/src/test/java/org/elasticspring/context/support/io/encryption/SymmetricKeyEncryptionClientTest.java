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

import org.elasticspring.support.TestStackEnvironment;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.DigestUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

/**
 * @author Alain Sahli
 * @since 1.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("SymmetricKeyEncryptionClientTest-context.xml")
public class SymmetricKeyEncryptionClientTest {

	private static final String S3_PREFIX = "s3://";
	@Autowired
	private TestStackEnvironment testStackEnvironment;

	@Autowired
	private ResourcePatternResolver resourceLoader;

	@Test
	@Ignore
	public void testWriteEncryptedObjects() throws Exception {
		String bucketName = this.testStackEnvironment.getByLogicalId("EmptyBucket");
		Resource resource = this.resourceLoader.getResource(S3_PREFIX + bucketName + "/test.txt");

		if (resource instanceof WritableResource) {
			WritableResource writableResource = (WritableResource) resource;
			OutputStream outputStream = writableResource.getOutputStream();
			MessageDigest md = MessageDigest.getInstance("MD5");
			try {
				outputStream = new DigestOutputStream(outputStream, md);
				for (int i = 0; i < 6; i++) {
					for (int j = 0; j < (1024 * 1024); j++) {
						outputStream.write("c".getBytes("UTF-8"));
					}
				}
			} finally {
				outputStream.close();
			}
			String originalMd5Checksum = DigestUtils.md5DigestAsHex(md.digest());

			Resource downloadedResource = this.resourceLoader.getResource(S3_PREFIX + bucketName + "/test.txt");
			InputStream downloadedInputStream = downloadedResource.getInputStream();

			md.reset();
			try {
				downloadedInputStream = new DigestInputStream(downloadedInputStream, md);
				while (downloadedInputStream.read() != -1) {
					// go through the input stream until EOF to compute MD5 checksum.
					// Dummy operation to avoid checkstyle error
					int a = 1;
				}
			} finally {
				downloadedInputStream.close();
			}

			String downloadedMd5Checksum = DigestUtils.md5DigestAsHex(md.digest());
			Assert.assertEquals(originalMd5Checksum, downloadedMd5Checksum);
		}
	}
}
