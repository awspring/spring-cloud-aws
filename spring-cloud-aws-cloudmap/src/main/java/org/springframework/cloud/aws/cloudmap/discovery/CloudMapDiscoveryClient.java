package org.springframework.cloud.aws.cloudmap.discovery;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import org.springframework.cloud.aws.cloudmap.model.discovery.CloudMapDiscoveryProperties;
import org.springframework.cloud.aws.cloudmap.model.CloudMapProperties;
import org.springframework.cloud.aws.cloudmap.CloudMapUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CloudMapDiscoveryClient implements DiscoveryClient {

	public static final String DESCRIPTION = "AWS CloudMap Discovery Client";
	private static final CloudMapUtils UTILS = CloudMapUtils.INSTANCE.getInstance();
	private final AWSServiceDiscovery serviceDiscovery;
	private final CloudMapProperties properties;

	public CloudMapDiscoveryClient(AWSServiceDiscovery serviceDiscovery, CloudMapProperties properties) {
		this.serviceDiscovery = serviceDiscovery;
		this.properties = properties;
	}

	@Override
	public int getOrder() {
		return DiscoveryClient.super.getOrder();
	}

	@Override
	public String description() {
		return DESCRIPTION;
	}

	@Override
	public List<String> getServices() {
		final List<CloudMapDiscoveryProperties> discoveryProperties = properties.getDiscovery().getDiscoveryList();
		if (discoveryProperties != null && !discoveryProperties.isEmpty())
			return UTILS.listServices(serviceDiscovery, discoveryProperties);

		return Collections.emptyList();
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		// Service ID maintained as <namespace>_<serviceId>
		String[] split = serviceId.split("@");
		if (split.length == 2)
			return UTILS.listInstances(serviceDiscovery, split[0], split[1])
				.stream()
				.map(UTILS::getServiceInstance)
				.collect(Collectors.toList());

		return Collections.emptyList();
	}
}
