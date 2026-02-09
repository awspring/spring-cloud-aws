package io.awspring.cloud.autoconfigure.config.appconfig;

import io.awspring.cloud.appconfig.AppConfigPropertySource;
import io.awspring.cloud.autoconfigure.config.BootstrapLoggingHelper;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.MapPropertySource;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;

import java.util.Collections;
import java.util.Map;

public class AppConfigConfigDataLoader implements ConfigDataLoader<AppConfigDataResource> {

	public AppConfigConfigDataLoader(DeferredLogFactory logFactory) {
		BootstrapLoggingHelper.reconfigureLoggers(logFactory,
			"io.awspring.cloud.appconfig.AppConfigPropertySource",
			"io.awspring.cloud.autoconfigure.config.appconfig.AppConfigPropertySources");
	}

	@Override
	@Nullable
	public ConfigData load(ConfigDataLoaderContext context, AppConfigDataResource resource) {
		// resource is disabled if appconfig integration is disabled via
		// spring.cloud.aws.appconfig.enabled=false
		if (resource.isEnabled()) {
			AppConfigDataClient appConfigDataClient = context.getBootstrapContext().get(AppConfigDataClient.class);
			AppConfigPropertySource propertySource = resource.getPropertySources()
				.createPropertySource(resource.getContext(), resource.isOptional(), appConfigDataClient);
			if (propertySource != null) {
				return new ConfigData(Collections.singletonList(propertySource));
			} else {
				return null;
			}
		} else {
			// create dummy empty config data
			return new ConfigData(
				Collections.singletonList(new MapPropertySource("aws-appconfig:" + context, Map.of())));
		}
	}
}
