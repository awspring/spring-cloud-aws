package io.awspring.cloud.autoconfigure.config.appconfig;

import io.awspring.cloud.autoconfigure.core.AwsClientCustomizer;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClientBuilder;

/**
 * @author Matej Nedic
 * @since 3.0.0
 */
public interface AwsAppConfigDataClientCustomizer extends AwsClientCustomizer<AppConfigDataClientBuilder> {
}
