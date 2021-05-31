package org.springframework.cloud.aws.web;

import com.amazonaws.auth.AWSCredentialsProvider;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class SigningClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
	private final RequestSigner signer;

	public SigningClientHttpRequestInterceptor(AWSCredentialsProvider awsCredentialsProvider, String regionName, String serviceName) {
		this.signer = new RequestSigner(awsCredentialsProvider, regionName, serviceName);
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		signer.signRequest(request, body);
		return execution.execute(request, body);
	}
}
