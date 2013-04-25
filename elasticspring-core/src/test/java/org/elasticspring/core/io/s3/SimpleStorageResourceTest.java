/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.core.io.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class SimpleStorageResourceTest {

	@Test
	public void testFileExists() throws Exception {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata("bucket", "object")).thenReturn(new ObjectMetadata());
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object");
		assertTrue(simpleStorageResource.exists());
	}

	@Test
	public void testFileDoesNotExist() throws Exception {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata("bucket", "object")).thenReturn(null);
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object");
		assertFalse(simpleStorageResource.exists());
	}

	@Test
	public void testContentLengthAndLastModified() throws Exception {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentLength(1234L);
		Date lastModified = new Date();
		objectMetadata.setLastModified(lastModified);
		when(amazonS3.getObjectMetadata("bucket", "object")).thenReturn(objectMetadata);
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object");
		assertEquals(1234L, simpleStorageResource.contentLength());
		assertEquals(lastModified.getTime(), simpleStorageResource.lastModified());
	}

	@Test
	public void testContentLengthForFileThatDoesNotExist() throws Exception {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata("bucket", "object")).thenReturn(null);
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object");
		try {
			simpleStorageResource.contentLength();
			fail("FileNotFoundException expected because s3 resource does not exist!");
		} catch (FileNotFoundException e) {
			assertTrue(e.getMessage().contains("not found"));
		}

		try {
			simpleStorageResource.lastModified();
			fail("FileNotFoundException expected because s3 resource does not exist!");
		} catch (FileNotFoundException e) {
			assertTrue(e.getMessage().contains("not found"));
		}
	}

	@Test
	public void testGetFileName() throws Exception {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata("bucket", "object")).thenReturn(null);
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object");
		assertEquals("object", simpleStorageResource.getFilename());
	}

	@Test
	public void testGetInputStream() throws Exception {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		ObjectMetadata objectMetadata = mock(ObjectMetadata.class);
		when(amazonS3.getObjectMetadata("bucket", "object")).thenReturn(objectMetadata);

		S3Object s3Object = new S3Object();
		s3Object.setObjectMetadata(objectMetadata);

		InputStream inputStream = mock(InputStream.class);
		when(inputStream.read()).thenReturn(42);
		s3Object.setObjectContent(inputStream);

		when(amazonS3.getObject("bucket", "object")).thenReturn(s3Object);

		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object");
		assertTrue(simpleStorageResource.exists());
		assertEquals(42, simpleStorageResource.getInputStream().read());
	}

	@Test
	public void testGetDescription() throws Exception {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "1", "2");
		String description = simpleStorageResource.getDescription();
		assertTrue(description.contains("bucket"));
		assertTrue(description.contains("object"));
		assertTrue(description.contains("1"));
		assertTrue(description.contains("2"));
	}


	@Test
	public void testWriteFile() throws Exception {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "123123123123", "2");
		final String messageContext = "myFileContent";
		when(amazonS3.putObject(eq("123123123123"), Matchers.eq("2"), any(InputStream.class), any(ObjectMetadata.class))).thenAnswer(new Answer<PutObjectResult>() {

			@Override
			public PutObjectResult answer(InvocationOnMock invocation) throws Throwable {
				assertEquals("123123123123", invocation.getArguments()[0]);
				assertEquals("2", invocation.getArguments()[1]);
				byte[] content = new byte[messageContext.length()];
				assertEquals(content.length, ((InputStream) invocation.getArguments()[2]).read(content));
				assertEquals(messageContext, new String(content));
				return new PutObjectResult();
			}
		});
		OutputStream outputStream = simpleStorageResource.getOutputStream();
		outputStream.write(messageContext.getBytes());
		outputStream.flush();
		outputStream.close();
	}

}
