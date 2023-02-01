/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.awspring.cloud.core.io.s3;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.Random;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import org.springframework.core.task.SyncTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 */
class SimpleStorageResourceTest {

	@Test
	void exists_withExistingObjectMetadata_returnsTrue() throws Exception {
		// Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(new ObjectMetadata());

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object",
				new SyncTaskExecutor());

		// Assert
		assertThat(simpleStorageResource.exists()).isTrue();
	}

	@Test
	void exists_withoutExistingObjectMetadata_returnsFalse() throws Exception {
		// Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(null);

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object",
				new SyncTaskExecutor());

		// Act
		assertThat(simpleStorageResource.exists()).isFalse();
	}

	@Test
	void contentLength_withExistingResource_returnsContentLengthOfObjectMetaData() throws Exception {
		// Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentLength(1234L);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(objectMetadata);

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object",
				new SyncTaskExecutor());

		// Assert
		assertThat(simpleStorageResource.contentLength()).isEqualTo(1234L);
	}

	@Test
	void lastModified_withExistingResource_returnsLastModifiedDateOfResource() throws Exception {
		// Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		ObjectMetadata objectMetadata = new ObjectMetadata();
		Date lastModified = new Date();
		objectMetadata.setLastModified(lastModified);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(objectMetadata);

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object",
				new SyncTaskExecutor());

		// Assert
		assertThat(simpleStorageResource.lastModified()).isEqualTo(lastModified.getTime());
	}

	@Test
	void contentLength_fileDoesNotExists_reportsError() throws Exception {
		// Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(null);
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object",
				new SyncTaskExecutor());

		// Assert
		assertThatThrownBy(simpleStorageResource::contentLength).isInstanceOf(FileNotFoundException.class)
				.hasMessageContaining("not found");

	}

	@Test
	void lastModified_fileDoestNotExist_reportsError() throws Exception {
		// Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(null);

		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object",
				new SyncTaskExecutor());

		// Assert
		assertThatThrownBy(simpleStorageResource::lastModified).isInstanceOf(FileNotFoundException.class)
				.hasMessageContaining("not found");
	}

	@Test
	void getFileName_existingObject_returnsFileNameWithoutBucketNameFromParameterWithoutActuallyFetchingTheFile()
			throws Exception {
		// Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(null);

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object",
				new SyncTaskExecutor());

		// Assert
		assertThat(simpleStorageResource.getFilename()).isEqualTo("object");
	}

	@Test
	void getInputStream_existingObject_returnsInputStreamWithContent() throws Exception {
		// Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		ObjectMetadata objectMetadata = mock(ObjectMetadata.class);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(objectMetadata);

		S3Object s3Object = new S3Object();
		s3Object.setObjectMetadata(objectMetadata);
		s3Object.setObjectContent(new ByteArrayInputStream(new byte[] { 42 }));
		when(amazonS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Object);

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object",
				new SyncTaskExecutor());

		// Assert
		assertThat(simpleStorageResource.exists()).isTrue();
		assertThat(simpleStorageResource.getInputStream().read()).isEqualTo(42);
	}

	@Test
	void getDescription_withoutObjectMetaData_returnsDescriptiveDescription() throws Exception {
		// Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "1", "2",
				new SyncTaskExecutor());
		String description = simpleStorageResource.getDescription();

		// Assert
		assertThat(description.contains("bucket")).isTrue();
		assertThat(description.contains("object")).isTrue();
		assertThat(description.contains("1")).isTrue();
		assertThat(description.contains("2")).isTrue();
	}

	@Test
	void getUrl_existingObject_returnsUrlWithS3Prefix() throws Exception {

		AmazonS3Client amazonS3 = mock(AmazonS3Client.class);

		when(amazonS3.getRegion()).thenReturn(Region.EU_Ireland);

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object",
				new SyncTaskExecutor());

		// Assert
		assertThat(simpleStorageResource.getURL())
				.isEqualTo(new URL("https://s3.eu-west-1.amazonaws.com/bucket/object"));

	}

	@Test
	void getUrl_existingObject_returnsUrlWithS3Scheme() throws Exception {

		AmazonS3Client amazonS3 = mock(AmazonS3Client.class);

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object",
				new SyncTaskExecutor());

		// Assert
		assertThat(simpleStorageResource.getS3Uri()).isEqualTo(new URI("s3://bucket/object"));

	}

	@Test
	void getFile_existingObject_throwsMeaningFullException() throws Exception {

		AmazonS3Client amazonS3 = mock(AmazonS3Client.class);

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object",
				new SyncTaskExecutor());

		// Assert

		assertThatThrownBy(simpleStorageResource::getFile).isInstanceOf(UnsupportedOperationException.class)
				.hasMessageContaining("getInputStream()");

	}

	@Test
	void createRelative_existingObject_returnsRelativeCreatedFile() throws IOException {

		// Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(new ObjectMetadata());
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "object",
				new SyncTaskExecutor());

		// Act
		SimpleStorageResource subObject = simpleStorageResource.createRelative("subObject");

		// Assert
		assertThat(subObject.getFilename()).isEqualTo("object/subObject");
	}

	@Test
	void createRelative_root_returnsRelativeCreatedFile() throws IOException {

		// Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		when(amazonS3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(new ObjectMetadata());
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucket", "",
				new SyncTaskExecutor());

		// Act
		SimpleStorageResource subObject = simpleStorageResource.createRelative("subObject");

		// Assert
		assertThat(subObject.getFilename()).isEqualTo("subObject");
	}

	@Test
	void writeFile_forNewFile_writesFileContent() throws Exception {
		// Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucketName", "objectName",
				new SyncTaskExecutor());
		String messageContext = "myFileContent";
		when(amazonS3.putObject(eq("bucketName"), eq("objectName"), any(InputStream.class), any(ObjectMetadata.class)))
				.thenReturn(new PutObjectResult());
		OutputStream outputStream = simpleStorageResource.getOutputStream();

		// Act
		outputStream.write(messageContext.getBytes());
		outputStream.flush();
		outputStream.close();

		// Assert
		ArgumentCaptor<InputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);
		ArgumentCaptor<ObjectMetadata> objectMetadataArgumentCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);
		verify(amazonS3).putObject(eq("bucketName"), eq("objectName"), inputStreamArgumentCaptor.capture(),
				objectMetadataArgumentCaptor.capture());
		byte[] content = new byte[messageContext.length()];
		assertThat(inputStreamArgumentCaptor.getValue().read(content)).isEqualTo(content.length);
		assertThat(new String(content)).isEqualTo(messageContext);
		assertThat(objectMetadataArgumentCaptor.getValue().getContentType()).isNull();
	}

	@Test
	void writeFile_simpleUpload_setsContentType() throws Exception {
		// Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucketName", "objectName",
				new SyncTaskExecutor(), null, "text/plain");
		String messageContext = "myFileContent";
		when(amazonS3.putObject(eq("bucketName"), eq("objectName"), any(InputStream.class), any(ObjectMetadata.class)))
				.thenReturn(new PutObjectResult());
		OutputStream outputStream = simpleStorageResource.getOutputStream();

		// Act
		outputStream.write(messageContext.getBytes());
		outputStream.flush();
		outputStream.close();

		// Assert

		ArgumentCaptor<ObjectMetadata> objectMetadataArgumentCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);
		verify(amazonS3).putObject(eq("bucketName"), eq("objectName"), any(InputStream.class),
				objectMetadataArgumentCaptor.capture());
		assertThat(objectMetadataArgumentCaptor.getValue().getContentType()).isEqualTo("text/plain");
	}

	@Test
	void writeFile_multipartUpload_setsContentType() throws Exception {
		// Arrange
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3, "bucketName", "objectName",
				new SyncTaskExecutor(), null, "text/plain");

		byte[] messageContext = new byte[(1024 * 1024 * 5) + 1];
		new Random().nextBytes(messageContext);

		when(amazonS3.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class)))
				.thenAnswer((Answer<InitiateMultipartUploadResult>) invocation -> {
					assertThat(((InitiateMultipartUploadRequest) invocation.getArguments()[0]).getBucketName())
							.isEqualTo("bucketName");
					assertThat(((InitiateMultipartUploadRequest) invocation.getArguments()[0]).getKey())
							.isEqualTo("objectName");
					assertThat(((InitiateMultipartUploadRequest) invocation.getArguments()[0]).getObjectMetadata()
							.getContentType()).isEqualTo("text/plain");
					return new InitiateMultipartUploadResult();
				});
		OutputStream outputStream = simpleStorageResource.getOutputStream();

		// Act
		outputStream.write(messageContext);
		outputStream.flush();

		// Assert
	}

	@Test
	void getUri_encodes_objectName() throws Exception {
		AmazonS3 s3 = mock(AmazonS3.class);
		when(s3.getRegion()).thenReturn(Region.US_West_2);
		SimpleStorageResource resource = new SimpleStorageResource(s3, "bucketName", "some/[objectName]",
				new SyncTaskExecutor());

		assertThat(resource.getURI())
				.isEqualTo(new URI("https://s3.us-west-2.amazonaws.com/bucketName/some%2F%5BobjectName%5D"));
	}

}
