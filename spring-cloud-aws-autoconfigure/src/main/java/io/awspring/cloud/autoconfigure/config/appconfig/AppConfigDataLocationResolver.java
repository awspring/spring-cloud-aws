package io.awspring.cloud.autoconfigure.config.appconfig;

import io.awspring.cloud.appconfig.RequestContext;
import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.config.AbstractAwsConfigDataLocationResolver;
import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.autoconfigure.core.CredentialsProperties;
import io.awspring.cloud.autoconfigure.core.RegionProperties;
import org.apache.commons.logging.Log;
import org.springframework.boot.bootstrap.BootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClientBuilder;

import java.util.ArrayList;
import java.util.List;

public class AppConfigDataLocationResolver extends AbstractAwsConfigDataLocationResolver<AppConfigDataResource> {

	/**
	 * AWS App Config Data prefix.
	 */
	public static final String PREFIX = "aws-appconfig:";

	private final Log log;

	public AppConfigDataLocationResolver(DeferredLogFactory deferredLogFactory) {
		this.log = deferredLogFactory.getLog(AppConfigDataLocationResolver.class);
	}

	@Override
	protected String getPrefix() {
		return PREFIX;
	}

	@Override
	public List<AppConfigDataResource> resolve(ConfigDataLocationResolverContext resolverContext,
											   ConfigDataLocation location) throws ConfigDataLocationNotFoundException {
		AppConfigProperties appConfigProperties = loadProperties(resolverContext.getBinder());
		List<AppConfigDataResource> locations = new ArrayList<>();
		AppConfigPropertySources propertySources = new AppConfigPropertySources();
		List<String> contexts = getCustomContexts(location.getNonPrefixedValue(PREFIX));


		if (appConfigProperties.isEnabled()) {
			registerBean(resolverContext, AwsProperties.class, loadAwsProperties(resolverContext.getBinder()));
			registerBean(resolverContext, AppConfigProperties.class, appConfigProperties);
			registerBean(resolverContext, CredentialsProperties.class,
				loadCredentialsProperties(resolverContext.getBinder()));
			registerBean(resolverContext, RegionProperties.class, loadRegionProperties(resolverContext.getBinder()));

			registerAndPromoteBean(resolverContext, AppConfigDataClient.class, this::createAppConfigDataClient);

			contexts.forEach(propertySourceContext -> locations
				.add(new AppConfigDataResource(resolveContext(propertySourceContext, appConfigProperties.getSeparator()), location.isOptional(), propertySources)));

			if (!location.isOptional() && locations.isEmpty()) {
				throw new AppConfigKeysMissingException(
					"No AppConfigData keys provided in `spring.config.import=aws-appconfig:` configuration.");
			}
		} else {
			// create dummy resources with enabled flag set to false,
			// because returned locations cannot be empty
			contexts.forEach(propertySourceContext -> locations.add(
				new AppConfigDataResource(resolveContext(propertySourceContext, appConfigProperties.getSeparator()), location.isOptional(), false, propertySources)));
		}
		return locations;
	}

	private RequestContext resolveContext(String propertySourceContext, String separator) {
		var response = propertySourceContext.split(java.util.regex.Pattern.quote(separator));
		return new RequestContext(response[0].trim(), response[1].trim(), response[2].trim(), propertySourceContext);
	}

	private AppConfigDataClient createAppConfigDataClient(BootstrapContext context) {
		AppConfigDataClientBuilder builder = configure(AppConfigDataClient.builder(), context.get(AppConfigProperties.class), context);

		try {
			AppConfigClientCustomizer appConfigClientCustomizer = context.get(AppConfigClientCustomizer.class);
			if (appConfigClientCustomizer != null) {
				appConfigClientCustomizer.customize(builder);
			}
		} catch (IllegalStateException e) {
			log.debug("Bean of type AwsSyncClientCustomizer is not registered: " + e.getMessage());
		}

		try {
			AwsSyncClientCustomizer awsSyncClientCustomizer = context.get(AwsSyncClientCustomizer.class);
			if (awsSyncClientCustomizer != null) {
				awsSyncClientCustomizer.customize(builder);
			}
		} catch (IllegalStateException e) {
			log.debug("Bean of type AwsSyncClientCustomizer is not registered: " + e.getMessage());
		}

		return builder.build();
	}


	protected AppConfigProperties loadProperties(Binder binder) {
		return binder.bind(AppConfigProperties.PREFIX, Bindable.of(AppConfigProperties.class)).orElseGet(AppConfigProperties::new);
	}
}
