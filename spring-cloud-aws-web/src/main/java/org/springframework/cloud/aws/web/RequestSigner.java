package org.springframework.cloud.aws.web;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import org.springframework.http.HttpRequest;

public class RequestSigner {
	private final String regionName;
	private final String serviceName;
	private final AWSCredentialsProvider awsCredentialsProvider;

	public RequestSigner(AWSCredentialsProvider awsCredentialsProvider, String regionName, String serviceName) {
		this.regionName = regionName;
		this.serviceName = serviceName;
		this.awsCredentialsProvider = awsCredentialsProvider;
	}

	public void signRequest(HttpRequest request, byte[] body) {
		AWS4Signer signer = this.createAws4Signer();
		signer.sign(new SpringSignableRequest(request, body), this.awsCredentialsProvider.getCredentials());
	}

	protected AWS4Signer createAws4Signer() {
		AWS4Signer signer = new AWS4Signer(false);
		signer.setRegionName(this.regionName);
		signer.setServiceName(this.serviceName);
		return signer;
	}
}
