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

package org.elasticspring.context.support.io;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Region;
import org.elasticspring.core.io.s3.S3Region;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class TestBucketsInitialization implements InitializingBean, DisposableBean {

	private static final String DEFAULT_FILENAME = "test.txt";
	private static final String NAME_WITH_DOTS = ".elasticspring.org";
	private static final String NAME_WITHOUT_DOTS = "-elasticspring-org";
	private final AmazonS3 amazonS3;

	@Autowired
	public TestBucketsInitialization(@SuppressWarnings("SpringJavaAutowiringInspection") AmazonS3 amazonS3) {
		this.amazonS3 = amazonS3;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.createTestBucketForEachRegion();
	}

	private void createTestBucketForEachRegion() {
		createBucketsWithAndWithoutDotsAndFile(S3Region.US_STANDARD.getLocation(), Region.US_Standard);
		createBucketsWithAndWithoutDotsAndFile(S3Region.OREGON.getLocation(), Region.US_West_2);
		createBucketsWithAndWithoutDotsAndFile(S3Region.NORTHERN_CALIFORNIA.getLocation(), Region.US_West);
		createBucketsWithAndWithoutDotsAndFile(S3Region.IRELAND.getLocation(), Region.EU_Ireland);
		createBucketsWithAndWithoutDotsAndFile(S3Region.SINGAPORE.getLocation(), Region.AP_Singapore);
		createBucketsWithAndWithoutDotsAndFile(S3Region.SYDNEY.getLocation(), Region.AP_Sydney);
		createBucketsWithAndWithoutDotsAndFile(S3Region.TOKYO.getLocation(), Region.AP_Tokyo);
		createBucketsWithAndWithoutDotsAndFile(S3Region.SAO_PAULO.getLocation(), Region.SA_SaoPaulo);
	}

	private void createBucketsWithAndWithoutDotsAndFile(String bucketName, Region region) {
		createBucketWithFile(bucketName.toLowerCase() + NAME_WITH_DOTS, region);
		createBucketWithFile(bucketName.toLowerCase() + NAME_WITHOUT_DOTS, region);
	}


	private void createBucketWithFile(String bucketName, Region region) {
		if (!isBucketAlreadyExisting(bucketName)) {
			this.amazonS3.createBucket(bucketName, region);
		}

		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentLength(bucketName.getBytes().length);
		this.amazonS3.putObject(bucketName, DEFAULT_FILENAME, new ByteArrayInputStream(bucketName.getBytes()), objectMetadata);
	}

	private boolean isBucketAlreadyExisting(String bucketName) {
		List<Bucket> buckets = this.amazonS3.listBuckets();
		for (Bucket bucket : buckets) {
			if (bucket.getName().equals(bucketName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void destroy() throws Exception {
		deleteBucketWithAndWithoutDotsAndFile(S3Region.US_STANDARD.getLocation());
		deleteBucketWithAndWithoutDotsAndFile(S3Region.OREGON.getLocation());
		deleteBucketWithAndWithoutDotsAndFile(S3Region.NORTHERN_CALIFORNIA.getLocation());
		deleteBucketWithAndWithoutDotsAndFile(S3Region.IRELAND.getLocation());
		deleteBucketWithAndWithoutDotsAndFile(S3Region.SINGAPORE.getLocation());
		deleteBucketWithAndWithoutDotsAndFile(S3Region.SYDNEY.getLocation());
		deleteBucketWithAndWithoutDotsAndFile(S3Region.TOKYO.getLocation());
		deleteBucketWithAndWithoutDotsAndFile(S3Region.SAO_PAULO.getLocation());
	}

	private void deleteBucketWithAndWithoutDotsAndFile(String bucketName) {
		deleteBucket(bucketName.toLowerCase() + NAME_WITH_DOTS);
		deleteBucket(bucketName.toLowerCase() + NAME_WITHOUT_DOTS);
	}

	private void deleteBucket(String bucketName) {
		this.amazonS3.deleteObject(bucketName, DEFAULT_FILENAME);
		this.amazonS3.deleteBucket(bucketName);
	}
}
