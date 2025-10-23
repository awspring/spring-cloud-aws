package io.awspring.cloud.autoconfigure.kinesis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static io.awspring.cloud.autoconfigure.kinesis.KinesisClientLibraryProperties.PREFIX;


@ConfigurationProperties(prefix = PREFIX)
public class KinesisClientLibraryProperties {

	public static final String PREFIX = "spring.cloud.aws.kinesis.client.library";

	private String streamName;
	private String applicationName;

	public String getStreamName() {
		return streamName;
	}

	public void setStreamName(String streamName) {
		this.streamName = streamName;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
}
