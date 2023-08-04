package io.awspring.cloud.sqs.annotation;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

public class SqsListenerReflectiveProcessorTest {


	private final SqsListenerReflectiveProcessor processor = new SqsListenerReflectiveProcessor();

	private final ReflectionHints hints = new ReflectionHints();

	@Test
	void registerClassAndListenerParameterForReflection() throws NoSuchMethodException {
		Method method = MyService.class.getDeclaredMethod("listen", MyService.SampleRecord.class);
		processor.registerReflectionHints(hints, method);
		assertThat(
			hints.typeHints())
			.satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType())
					.isEqualTo(TypeReference.of(MyService.class)),
				typeHint -> {
					assertThat(typeHint.getType())
						.isEqualTo(TypeReference.of(MyService.SampleRecord.class));
					assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
						MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
						MemberCategory.DECLARED_FIELDS);
					assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
						hint -> assertThat(hint.getName()).isEqualTo("propertyOne"),
						hint -> assertThat(hint.getName()).isEqualTo("propertyTwo"));
				},
				typeHint -> assertThat(typeHint.getType())
					.isEqualTo(TypeReference.of(String.class)));
	}
	@Service
	public class MyService {

		@SqsListener(queueNames = "myQueueName")
		public void listen(SampleRecord message) {
		}

		private record SampleRecord(String propertyOne, String propertyTwo) {
		}

	}
}
