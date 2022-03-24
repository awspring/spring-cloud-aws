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

package io.awspring.cloud.s3.sample;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringCloudAwsS3Sample {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudAwsS3Sample.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudAwsS3Sample.class, args);
	}

	@Bean
	ApplicationRunner applicationRunner(S3Client s3Client) {
		return args -> {
			s3Client.listBuckets().buckets().forEach(bucket -> {
				List<S3Object> contents = s3Client.listObjects(r -> r.bucket(bucket.name())).contents();
				System.out.println("Bucket " + bucket.name() + " has " + contents.size() + " items");
			});
		};
	}
}
