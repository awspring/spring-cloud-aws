package io.awspring.cloud.autoconfigure.config.appconfig;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import io.awspring.cloud.autoconfigure.config.reload.ReloadProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import static io.awspring.cloud.autoconfigure.config.appconfig.AppConfigProperties.PREFIX;

/**
 * Configuration properties for AWS AppConfig integration.
 * @author Matej Nedic
 * @since 4.1.0
 */
@ConfigurationProperties(prefix = PREFIX)
public class AppConfigProperties extends AwsClientProperties {

	/**
	 * The prefix used for App Config related properties.
	 */
	public static final String PREFIX = "spring.cloud.aws.appconfig";

	/**
	 * Enables App Config import integration.
	 */
	private boolean enabled = true;

	/**
	 * Properties related to configuration reload.
	 */
	@NestedConfigurationProperty
	private ReloadProperties reload = new ReloadProperties();


	private String separator = "#";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String  getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public ReloadProperties getReload() {
		return reload;
	}

	public void setReload(ReloadProperties reload) {
		this.reload = reload;
	}
}
