package org.springframework.cloud.aws.cloudmap.http;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;

@Service
public class CloudMapRequestFactory implements ClientHttpRequestFactory {
	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return null;
	}
}
