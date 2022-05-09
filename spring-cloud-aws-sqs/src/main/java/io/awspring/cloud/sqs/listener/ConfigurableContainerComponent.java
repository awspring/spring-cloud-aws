package io.awspring.cloud.sqs.listener;

/**
 * Representation of a {@link MessageListenerContainer} component
 * that can be configured using a {@link ContainerOptions} instance.
 * Note that the provided options should be a copy, so any changes
 * on it will have no effect on the container.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface ConfigurableContainerComponent {

	default void configure(ContainerOptions containerOptions) {
	}

}
