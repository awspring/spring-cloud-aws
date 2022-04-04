package org.springframework.cloud.aws.cloudmap.discovery;

import com.amazonaws.services.servicediscovery.model.HttpInstanceSummary;
import org.springframework.cloud.aws.cloudmap.CloudMapUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Map;

public class CloudMapServiceInstance implements ServiceInstance {

	private final CloudMapUtils UTILS = CloudMapUtils.INSTANCE.getInstance();

	HttpInstanceSummary instanceSummary;

	public CloudMapServiceInstance(HttpInstanceSummary httpInstanceSummary) {
		this.instanceSummary = httpInstanceSummary;
	}

	@Override
	public String getInstanceId() {
		return instanceSummary.getInstanceId();
	}

	@Override
	public String getScheme() {
		return getUri().getScheme();
	}

	@Override
	public String getServiceId() {
		return UTILS.generateServiceId(instanceSummary.getNamespaceName(), instanceSummary.getServiceName());
	}

	@Override
	public String getHost() {
		return instanceSummary.getAttributes().get("AWS_INSTANCE_IPV4");
	}

	@Override
	public int getPort() {
		String port = instanceSummary.getAttributes().get("AWS_INSTANCE_PORT");
		return StringUtils.hasText(port) ? Integer.parseInt(port) : 0;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public URI getUri() {
		return URI.create(String.format("http://%s:%s", this.getHost(), this.getPort()));
	}

	@Override
	public Map<String, String> getMetadata() {
		return instanceSummary.getAttributes();
	}
}
