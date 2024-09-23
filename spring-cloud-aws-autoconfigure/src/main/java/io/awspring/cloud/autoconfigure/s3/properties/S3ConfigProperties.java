package io.awspring.cloud.autoconfigure.s3.properties;

import io.awspring.cloud.autoconfigure.config.reload.ReloadProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class S3ConfigProperties {

	@NestedConfigurationProperty
	private ReloadProperties reload = new ReloadProperties();

	private boolean enabled = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}


	public ReloadProperties getReload() {
		return reload;
	}

	public void setReload(ReloadProperties reload) {
		this.reload = reload;
	}
}
