package org.springframework.cloud.aws.web;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;

import java.net.URI;
import java.sql.Date;
import java.time.Instant;

/**
 * the test values are taken from the aws-sig-v4-test-suite
 */
class RequestSignerTest {
	private final AWSCredentialsProvider credentials = new AWSStaticCredentialsProvider(
		new BasicAWSCredentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY"));

	@Test
	void sign_request_without_body() {
		RequestSigner signer = new RequestSigner(this.credentials, "us-east-1", "service") {
			@Override
			protected AWS4Signer createAws4Signer() {
				AWS4Signer aws4Signer = super.createAws4Signer();
				aws4Signer.setOverrideDate(Date.from(Instant.parse("2015-08-30T12:36:00Z")));
				return aws4Signer;
			}
		};

		HttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://example.amazonaws.com/?Param2=value2&Param1=value1"));
		signer.signRequest(request, null);

		assertThat(request.getHeaders()).containsEntry("x-amz-date", singletonList("20150830T123600Z"))
			.containsEntry("Host", singletonList("example.amazonaws.com"))
			.containsEntry("Authorization", singletonList(
				"AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20150830/us-east-1/service/aws4_request, SignedHeaders=host;x-amz-date, Signature=b97d918cfa904a5beff61c982a1b6f458b799221646efd99d3219ec94cdf2500")
			);
	}

	@Test
	void sign_request_with_body() {
		RequestSigner signer = new RequestSigner(this.credentials, "us-east-1", "service") {
			@Override
			protected AWS4Signer createAws4Signer() {
				AWS4Signer aws4Signer = super.createAws4Signer();
				aws4Signer.setOverrideDate(Date.from(Instant.parse("2015-08-30T12:36:00Z")));
				return aws4Signer;
			}
		};

		HttpRequest request = new MockClientHttpRequest(HttpMethod.POST, URI.create("http://example.amazonaws.com/"));
		request.getHeaders().add("Content-Type", "application/x-www-form-urlencoded");
		signer.signRequest(request, "Param1=value1".getBytes());

		assertThat(request.getHeaders()).containsEntry("x-amz-date", singletonList("20150830T123600Z"))
			.containsEntry("Host", singletonList("example.amazonaws.com"))
			.containsEntry("Authorization", singletonList(
				"AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20150830/us-east-1/service/aws4_request, SignedHeaders=content-type;host;x-amz-date, Signature=ff11897932ad3f4e8b18135d722051e5ac45fc38421b1da7b9d196a0fe09473a")
			);
	}
}
