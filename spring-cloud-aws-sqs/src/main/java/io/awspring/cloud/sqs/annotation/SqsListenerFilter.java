package io.awspring.cloud.sqs.annotation;

/**
 * Predicate interface to filter {@link SqsListener} annotations during bean post-processing.
 */
@FunctionalInterface
public interface SqsListenerFilter {

	boolean createEndpoint(SqsListener annotation);
}
