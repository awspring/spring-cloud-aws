package org.springframework.cloud.aws.cloudmap.registration;

import org.springframework.cloud.aws.cloudmap.CloudMapUtils;
import org.springframework.cloud.aws.cloudmap.model.registration.CloudMapRegistryProperties;
import org.springframework.cloud.client.serviceregistry.Registration;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

public class ServiceRegistration implements Registration {
	private final CloudMapRegistryProperties properties;
	private final Map<String, String> registrationDetails;
	private final CloudMapUtils UTILS = CloudMapUtils.INSTANCE.getInstance();
	
	public ServiceRegistration(CloudMapRegistryProperties properties) {
		registrationDetails = UTILS.getRegistrationAttributes();
		this.properties = properties;
	}

	@Override
	public String getInstanceId() {
		return UUID.randomUUID().toString();
	}

	@Override
	public String getScheme() {
		return Registration.super.getScheme();
	}

	@Override
	public String getServiceId() {
		return UTILS.generateServiceId(properties.getNameSpace(), properties.getService());
	}

	@Override
	public String getHost() {
		return registrationDetails.get(UTILS.IPV_4_ADDRESS);
	}

	@Override
	public int getPort() {
		return 0;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public URI getUri() {
		return null;
	}

	@Override
	public Map<String, String> getMetadata() {
		return registrationDetails;
	}
}
