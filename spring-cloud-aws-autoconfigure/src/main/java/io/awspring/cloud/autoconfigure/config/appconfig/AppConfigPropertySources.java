package io.awspring.cloud.autoconfigure.config.appconfig;

import io.awspring.cloud.appconfig.AppConfigPropertySource;
import io.awspring.cloud.appconfig.RequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;

public class AppConfigPropertySources {
	private static final Log LOG = LogFactory.getLog(AppConfigPropertySources.class);

	@Nullable
	public AppConfigPropertySource createPropertySource(RequestContext context, boolean optional, AppConfigDataClient client) {
		Assert.notNull(context, "RequestContext is required");
		Assert.notNull(client, "AppConfigDataClient is required");

		LOG.info("Loading properties from AWS App Config: " + context + ", optional: " + optional);
		try {
			AppConfigPropertySource propertySource = new AppConfigPropertySource(context, client);
			propertySource.init();
			return propertySource;
		} catch (Exception e) {
			LOG.warn("Unable to load AWS App Config from " + context + ". " + e.getMessage());
			if (!optional) {
				throw new AwsAppConfigPropertySourceNotFoundException(e);
			}
		}
		return null;
	}
}
