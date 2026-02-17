# Spring Cloud AWS SQS - Architectural Overview

**What:** This module integrates Amazon SQS with Spring applications, providing annotation-driven listeners (via [@SqsListener](src/main/java/io/awspring/cloud/sqs/annotation/SqsListener.java) and programmatic listener containers, backed by an asynchronous processing runtime.

**Why:** The SQS integration underwent a major redesign in Spring Cloud AWS 3.0 to address limitations in previous versions and build on AWS SDK v2’s async API, and it was [announced as GA in 2023](https://spring.io/blog/2023/05/02/announcing-spring-cloud-aws-3-0-0).

**Who:** This document is meant for maintainers, contributors, and readers who want to understand the module’s internal structure and design. It describes the module in two phases: an assembly phase at startup, and a container execution phase where messages are polled, processed, and acknowledged. It focuses on the high-level structure and provides shared terminology for discussing the module’s flows and components.

## Two-Phase Architecture

The module is organized into two phases with different responsibilities:

- **Assembly phase**: At startup, Spring detects [@SqsListener](src/main/java/io/awspring/cloud/sqs/annotation/SqsListener.java) annotations, creates listener endpoints, and wires [MessageListenerContainer](src/main/java/io/awspring/cloud/sqs/listener/MessageListenerContainer.java) instances through a factory and registry. This is similar to patterns used by Spring for Apache Kafka and other Spring messaging projects.

- **Container execution phase**: When containers start, they run an asynchronous pipeline that polls SQS, invokes the listener, and acknowledges messages. This pipeline builds on AWS SDK v2’s async API ([SqsAsyncClient](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/sqs/SqsAsyncClient.html)) and uses a composable component model, including adaptive backpressure controls. While reusing familiar Spring abstractions such as [MessageListener](src/main/java/io/awspring/cloud/sqs/listener/MessageListener.java), the async processing pipeline is a module-specific design introduced in Spring Cloud AWS 3.0.

This separation keeps startup wiring concerns independent from message processing concerns, and makes the runtime pipeline easier to reason about and evolve without changing the assembly flow.

## Assembly Phase

The assembly phase wires listener containers at startup. The flow is:

1. [SqsListenerAnnotationBeanPostProcessor](src/main/java/io/awspring/cloud/sqs/annotation/SqsListenerAnnotationBeanPostProcessor.java) detects [@SqsListener](src/main/java/io/awspring/cloud/sqs/annotation/SqsListener.java) annotations during bean post-processing
2. For each annotation, it creates an [Endpoint](src/main/java/io/awspring/cloud/sqs/config/Endpoint.java) describing the listener
3. Endpoints are registered with the [EndpointRegistrar](src/main/java/io/awspring/cloud/sqs/config/EndpointRegistrar.java)
4. The registrar delegates to [SqsMessageListenerContainerFactory](src/main/java/io/awspring/cloud/sqs/config/SqsMessageListenerContainerFactory.java) to create containers
5. Containers are registered in the [MessageListenerContainerRegistry](src/main/java/io/awspring/cloud/sqs/listener/MessageListenerContainerRegistry.java), which manages their lifecycle

```mermaid
flowchart LR
	A["SqsListenerAnnotationBeanPostProcessor"] --> B["Endpoint\ncreated"]
	B --> C["EndpointRegistrar"]
	C --> D["SqsMessageListenerContainerFactory"]
	D --> E["MessageListenerContainer"]
	E --> F["MessageListenerContainerRegistry"]
	F --> G["Container lifecycle start\n(transition to container execution)"]
```

## Container Execution Phase

When the [MessageListenerContainerRegistry](src/main/java/io/awspring/cloud/sqs/listener/MessageListenerContainerRegistry.java) starts its containers, each container assembles its processing pipeline and begins polling for messages. The [ContainerComponentFactory](src/main/java/io/awspring/cloud/sqs/listener/ContainerComponentFactory.java) creates the runtime components and wires them together.

### Async execution model

SQS is inherently I/O-bound. Every poll is a network call to AWS, and every acknowledgement is a batch-delete call. A blocking model would tie up threads waiting on these responses.

The pipeline is built on [SqsAsyncClient](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/sqs/SqsAsyncClient.html), where `receiveMessage()` and `deleteMessageBatch()` both return `CompletableFuture`. This keeps polling and acknowledgement non-blocking and makes concurrency primarily a matter of configured in-flight capacity rather than thread availability.

### Composable Pipeline

The runtime is structured as a composable pipeline. Within each [listener container](src/main/java/io/awspring/cloud/sqs/listener/AbstractPipelineMessageListenerContainer.java):

- [**MessageSource**](src/main/java/io/awspring/cloud/sqs/listener/source/MessageSource.java): Polls SQS for messages and converts them to Spring [Message](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/messaging/Message.html) objects. Uses a [BackPressureHandler](src/main/java/io/awspring/cloud/sqs/listener/backpressure/BackPressureHandler.java) to gate polling based on in-flight message capacity
- [**MessageSink**](src/main/java/io/awspring/cloud/sqs/listener/sink/MessageSink.java): Dispatches messages to the processing pipeline. Composable component with implementations such as [FanOutMessageSink](src/main/java/io/awspring/cloud/sqs/listener/sink/FanOutMessageSink.java) (single-message), [BatchMessageSink](src/main/java/io/awspring/cloud/sqs/listener/sink/BatchMessageSink.java), [OrderedMessageSink](src/main/java/io/awspring/cloud/sqs/listener/sink/OrderedMessageSink.java), and [MessageGroupingSinkAdapter](src/main/java/io/awspring/cloud/sqs/listener/sink/adapter/MessageGroupingSinkAdapter.java) (FIFO)
- [**MessageProcessingPipeline**](src/main/java/io/awspring/cloud/sqs/listener/pipeline/MessageProcessingPipeline.java): Chains together the stages that process each message:
  - [MessageInterceptor](src/main/java/io/awspring/cloud/sqs/listener/interceptor/MessageInterceptor.java) - before/after processing hooks
  - [MessageListener](src/main/java/io/awspring/cloud/sqs/listener/MessageListener.java) - invokes the user's [@SqsListener](src/main/java/io/awspring/cloud/sqs/annotation/SqsListener.java) method
  - [ErrorHandler](src/main/java/io/awspring/cloud/sqs/listener/errorhandler/ErrorHandler.java) - handles processing failures
  - [AcknowledgementHandler](src/main/java/io/awspring/cloud/sqs/listener/acknowledgement/handler/AcknowledgementHandler.java) - triggers acknowledgement (deletion from SQS)
- [**AcknowledgementProcessor**](src/main/java/io/awspring/cloud/sqs/listener/acknowledgement/AcknowledgementProcessor.java): Acknowledges processed messages by deleting them from SQS
- [**AcknowledgementResultCallback**](src/main/java/io/awspring/cloud/sqs/listener/acknowledgement/AcknowledgementResultCallback.java): Notified after acknowledgement succeeds or fails

These components are assembled at container start and interact through interfaces, which makes it possible to swap or extend individual stages.

```mermaid
flowchart LR
    A["MessageSource\n(polls SQS)"] --> B["MessageSink"]
    A --> BP["BackPressureHandler"]
    B --> P
    subgraph P ["MessageProcessingPipeline"]
        direction LR
        P1["Interceptor\n(before)"] --> P2["Listener"]
        P2 --> P3["ErrorHandler"]
        P3 --> P4["Interceptor\n(after)"]
        P4 --> P5["AcknowledgementHandler"]
    end
    P --> D["AcknowledgementProcessor\n(deletes from SQS)"]
    D --> E["AcknowledgementResultCallback"]
```
