package org.springframework.cloud.aws.web;

import com.amazonaws.ReadLimitInfo;
import com.amazonaws.SignableRequest;
import com.amazonaws.http.HttpMethodName;
import org.springframework.http.HttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class SpringSignableRequest implements SignableRequest<HttpRequest> {
	private final Map<String, List<String>> parameters;
	private final URI endpoint;
	private final HttpRequest request;
	private final InputStream content;

	public SpringSignableRequest(HttpRequest request, byte[] body) {
		Assert.notNull(request, "request must not be null");
		this.request = request;
		this.content = new ByteArrayInputStream(body != null ? body : new byte[0]);
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpRequest(this.request);
		this.parameters = uriBuilder.build().getQueryParams();
		this.endpoint = uriBuilder.replacePath(null).replaceQuery(null).build().toUri();
	}

	@Override
	public void addHeader(String name, String value) {
		this.request.getHeaders().add(name, value);
	}

	@Override
	public void addParameter(String name, String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setContent(InputStream inputStream) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, String> getHeaders() {
		return request.getHeaders().toSingleValueMap();
	}

	@Override
	public String getResourcePath() {
		return request.getURI().getRawPath();
	}

	@Override
	public Map<String, List<String>> getParameters() {
		return this.parameters;
	}

	@Override
	public URI getEndpoint() {
		return this.endpoint;
	}

	@Override
	public HttpMethodName getHttpMethod() {
		return HttpMethodName.fromValue(Objects.toString(request.getMethod(), null));
	}

	@Override
	public int getTimeOffset() {
		return 0;
	}

	@Override
	public InputStream getContent() {
		return this.content;
	}

	@Override
	public InputStream getContentUnwrapped() {
		return this.content;
	}

	@Override
	public ReadLimitInfo getReadLimitInfo() {
		return null;
	}

	@Override
	public HttpRequest getOriginalRequestObject() {
		return request;
	}
}
