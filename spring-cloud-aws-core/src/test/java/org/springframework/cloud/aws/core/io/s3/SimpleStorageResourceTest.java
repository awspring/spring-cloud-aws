/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.aws.core.io.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.core.task.SyncTaskExecutor;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 */
public class SimpleStorageResourceTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void exists_withExistingObjectMetadata_returnsTrue() throws Exception {
		//Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(new ObjectMetadata());

		//Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object", new SyncTaskExecutor());

		//Assert
		assertTrue(simpleStorageResource.exists());
	}

	@Test
	public void exists_withoutExistingObjectMetadata_returnsFalse() throws Exception {
		//Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(null);

		//Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object", new SyncTaskExecutor());

		//Act
		assertFalse(simpleStorageResource.exists());
	}

	@Test
	public void contentLength_withExistingResource_returnsContentLengthOfObjectMetaData() throws Exception {
		//Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentLength(1234L);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(objectMetadata);

		//Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object", new SyncTaskExecutor());

		//Assert
		assertEquals(1234L, simpleStorageResource.contentLength());
	}

	@Test
	public void lastModified_withExistingResource_returnsLastModifiedDateOfResource() throws Exception {
		//Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		ObjectMetadata objectMetadata = new ObjectMetadata();
		Date lastModified = new Date();
		objectMetadata.setLastModified(lastModified);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(objectMetadata);

		//Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object", new SyncTaskExecutor());

		//Assert
		assertEquals(lastModified.getTime(), simpleStorageResource.lastModified());
	}

	@Test
	public void contentLength_fileDoesNotExists_reportsError() throws Exception {
		//Arrange
		this.expectedException.expect(FileNotFoundException.class);
		this.expectedException.expectMessage("not found");

		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(null);
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object", new SyncTaskExecutor());

		//Act
		simpleStorageResource.contentLength();

		//Assert
	}


	@Test
	public void lastModified_fileDoestNotExist_reportsError() throws Exception {
		//Arrange
		this.expectedException.expect(FileNotFoundException.class);
		this.expectedException.expectMessage("not found");

		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(null);
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object", new SyncTaskExecutor());

		//Act
		simpleStorageResource.lastModified();

		//Assert
	}

	@Test
	public void getFileName_existingObject_returnsFileNameWithoutBucketNameFromParameterWithoutActuallyFetchingTheFile() throws Exception {
		//Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(null);

		//Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object", new SyncTaskExecutor());

		//Assert
		assertEquals("object", simpleStorageResource.getFilename());
	}

	@Test
	public void getInputStream_existingObject_returnsInputStreamWithContent() throws Exception {
		//Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		ObjectMetadata objectMetadata = mock(ObjectMetadata.class);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(objectMetadata);

		S3Object s3Object = new S3Object();
		s3Object.setObjectMetadata(objectMetadata);
		s3Object.setObjectContent(new ByteArrayInputStream(new byte[]{42}));
		when(amazonS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Object);

		//Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object", new SyncTaskExecutor());

		//Assert
		assertTrue(simpleStorageResource.exists());
		assertEquals(42, simpleStorageResource.getInputStream().read());
	}

	@Test
	public void getDescription_withoutObjectMetaData_returnsDescriptiveDescription() throws Exception {
		//Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);

		//Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "1", "2", new SyncTaskExecutor());
		String description = simpleStorageResource.getDescription();

		//Assert
		assertTrue(description.contains("bucket"));
		assertTrue(description.contains("object"));
		assertTrue(description.contains("1"));
		assertTrue(description.contains("2"));
	}

	@Test
	public void getUrl_existingObject_returnsUrlWithS3Prefix() throws Exception {

		AmazonS3Client amazonS3 = mock(AmazonS3Client.class);

		when(amazonS3.getRegion()).thenReturn(Region.EU_Ireland);

		//Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object", new SyncTaskExecutor());

		//Assert
		assertEquals(new URL("https://s3-eu-west-1.amazonaws.com/bucket/object"), simpleStorageResource.getURL());

	}

	@Test
	public void getFile_existingObject_throwsMeaningFullException() throws Exception {

		this.expectedException.expect(UnsupportedOperationException.class);
		this.expectedException.expectMessage("getInputStream()");

		AmazonS3Client amazonS3 = mock(AmazonS3Client.class);

		//Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object", new SyncTaskExecutor());

		//Assert
		simpleStorageResource.getFile();

	}

	@Test
	public void createRelative_existingObject_returnsRelativeCreatedFile() throws IOException {

		//Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(new ObjectMetadata());
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object", new SyncTaskExecutor());

		//Act
		SimpleStorageResource subObject = simpleStorageResource.createRelative("subObject");

		//Assert
		assertEquals("object/subObject", subObject.getFilename());
	}

	@Test
	public void writeFile_forNewFile_writesFileContent() throws Exception {
		//Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucketName", "objectName", new SyncTaskExecutor());
		final String messageContext = "myFileContent";
		when(amazonS3.putObject(eq("bucketName"), eq("objectName"), any(InputStream.class), any(ObjectMetadata.class))).thenAnswer(new Answer<PutObjectResult>() {

			@Override
			public PutObjectResult answer(InvocationOnMock invocation) throws Throwable {
				assertEquals("bucketName", invocation.getArguments()[0]);
				assertEquals("objectName", invocation.getArguments()[1]);
				byte[] content = new byte[messageContext.length()];
				assertEquals(content.length, ((InputStream) invocation.getArguments()[2]).read(content));
				assertEquals(messageContext, new String(content));
				return new PutObjectResult();
			}
		});
		OutputStream outputStream = simpleStorageResource.getOutputStream();

		//Act
		outputStream.write(messageContext.getBytes());
		outputStream.flush();
		outputStream.close();

		//Assert
	}
}