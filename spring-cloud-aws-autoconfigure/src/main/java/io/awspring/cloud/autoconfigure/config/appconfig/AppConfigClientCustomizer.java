package io.awspring.cloud.autoconfigure.config.appconfig;

import io.awspring.cloud.autoconfigure.AwsClientCustomizer;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClientBuilder;

/**
 * Callback interface that can be used to customize a {@link AppConfigDataClientBuilder}.
 *
 * @author Matej Nedic
 */
public interface AppConfigClientCustomizer extends AwsClientCustomizer<AppConfigDataClientBuilder> {
}
