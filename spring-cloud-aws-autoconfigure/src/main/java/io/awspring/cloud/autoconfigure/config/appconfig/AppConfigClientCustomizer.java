package io.awspring.cloud.autoconfigure.config.appconfig;

import io.awspring.cloud.autoconfigure.AwsClientCustomizer;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClientBuilder;

public interface AppConfigClientCustomizer extends AwsClientCustomizer<AppConfigDataClientBuilder> {
}
