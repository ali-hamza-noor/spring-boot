/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.tracing;

import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelHttpClientHandler;
import io.micrometer.tracing.otel.bridge.OtelHttpServerHandler;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.OtelTracer.EventPublisher;
import io.micrometer.tracing.otel.bridge.Slf4JBaggageEventListener;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OpenTelemetryAutoConfiguration}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class OpenTelemetryAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(OpenTelemetryAutoConfiguration.class));

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(OtelTracer.class);
			assertThat(context).hasSingleBean(EventPublisher.class);
			assertThat(context).hasSingleBean(OtelCurrentTraceContext.class);
			assertThat(context).hasSingleBean(OtelHttpClientHandler.class);
			assertThat(context).hasSingleBean(OtelHttpServerHandler.class);
			assertThat(context).hasSingleBean(OpenTelemetry.class);
			assertThat(context).hasSingleBean(SdkTracerProvider.class);
			assertThat(context).hasSingleBean(ContextPropagators.class);
			assertThat(context).hasSingleBean(Sampler.class);
			assertThat(context).hasSingleBean(Tracer.class);
		});
	}

	@ParameterizedTest
	@ValueSource(strings = { "io.micrometer.tracing.otel", "io.opentelemetry.sdk", "io.opentelemetry.api" })
	void shouldNotSupplyBeansIfDependencyIsMissing(String packageName) {
		this.contextRunner.withClassLoader(new FilteredClassLoader(packageName)).run((context) -> {
			assertThat(context).doesNotHaveBean(OtelTracer.class);
			assertThat(context).doesNotHaveBean(EventPublisher.class);
			assertThat(context).doesNotHaveBean(OtelCurrentTraceContext.class);
			assertThat(context).doesNotHaveBean(OtelHttpClientHandler.class);
			assertThat(context).doesNotHaveBean(OtelHttpServerHandler.class);
			assertThat(context).doesNotHaveBean(OpenTelemetry.class);
			assertThat(context).doesNotHaveBean(SdkTracerProvider.class);
			assertThat(context).doesNotHaveBean(ContextPropagators.class);
			assertThat(context).doesNotHaveBean(Sampler.class);
			assertThat(context).doesNotHaveBean(SpanProcessor.class);
			assertThat(context).doesNotHaveBean(Tracer.class);
		});
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customOtelTracer");
			assertThat(context).hasSingleBean(OtelTracer.class);
			assertThat(context).hasBean("customEventPublisher");
			assertThat(context).hasSingleBean(EventPublisher.class);
			assertThat(context).hasBean("customOtelCurrentTraceContext");
			assertThat(context).hasSingleBean(OtelCurrentTraceContext.class);
			assertThat(context).hasBean("customOtelHttpClientHandler");
			assertThat(context).hasSingleBean(OtelHttpClientHandler.class);
			assertThat(context).hasBean("customOtelHttpServerHandler");
			assertThat(context).hasSingleBean(OtelHttpServerHandler.class);
			assertThat(context).hasBean("customOpenTelemetry");
			assertThat(context).hasSingleBean(OpenTelemetry.class);
			assertThat(context).hasBean("customSdkTracerProvider");
			assertThat(context).hasSingleBean(SdkTracerProvider.class);
			assertThat(context).hasBean("customContextPropagators");
			assertThat(context).hasSingleBean(ContextPropagators.class);
			assertThat(context).hasBean("customSampler");
			assertThat(context).hasSingleBean(Sampler.class);
			assertThat(context).hasBean("customSpanProcessor");
			assertThat(context).hasBean("customTracer");
			assertThat(context).hasSingleBean(Tracer.class);
		});
	}

	@Test
	void shouldSupplyBaggageAndSlf4jEventListenersWhenMdcOnClasspath() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Slf4JEventListener.class);
			assertThat(context).hasSingleBean(Slf4JBaggageEventListener.class);
		});
	}

	@Test
	void shouldSupplySlf4jEventListenersWhenMdcOnClasspathAndBaggageCorrelationDisabled() {
		this.contextRunner.withPropertyValues("management.tracing.baggage.correlation.enabled=false").run((context) -> {
			assertThat(context).hasSingleBean(Slf4JEventListener.class);
			assertThat(context).doesNotHaveBean(Slf4JBaggageEventListener.class);
		});
	}

	@Test
	void shouldSupplySlf4jEventListenersWhenMdcOnClasspathAndBaggageDisabled() {
		this.contextRunner.withPropertyValues("management.tracing.baggage.enabled=false").run((context) -> {
			assertThat(context).hasSingleBean(Slf4JEventListener.class);
			assertThat(context).doesNotHaveBean(Slf4JBaggageEventListener.class);
		});
	}

	@Test
	void shouldNotSupplySlf4jEventListenersWhenMdcNotOnClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("org.slf4j")).run((context) -> {
			assertThat(context).doesNotHaveBean(Slf4JEventListener.class);
			assertThat(context).doesNotHaveBean(Slf4JBaggageEventListener.class);
		});
	}

	@Test
	void shouldSupplyB3PropagationIfPropagationPropertySet() {
		this.contextRunner.withPropertyValues("management.tracing.propagation.type=B3").run((context) -> {
			assertThat(context).hasSingleBean(B3Propagator.class);
			assertThat(context).hasBean("b3TextMapPropagator");
			assertThat(context).doesNotHaveBean(W3CTraceContextPropagator.class);
		});
	}

	@Test
	void shouldSupplyW3CPropagationWithBaggageByDefault() {
		this.contextRunner.run((context) -> assertThat(context).hasBean("w3cTextMapPropagatorWithBaggage"));
	}

	@Test
	void shouldSupplyW3CPropagationWithoutBaggageWhenDisabled() {
		this.contextRunner.withPropertyValues("management.tracing.baggage.enabled=false")
				.run((context) -> assertThat(context).hasBean("w3cTextMapPropagatorWithoutBaggage"));
	}

	@Test
	void shouldSupplyB3PropagationWithoutBaggageWhenBaggageDisabledAndB3PropagationEnabled() {
		this.contextRunner.withPropertyValues("management.tracing.baggage.enabled=false",
				"management.tracing.propagation.type=B3").run((context) -> {
					assertThat(context).hasBean("b3TextMapPropagator");
					assertThat(context).hasSingleBean(B3Propagator.class);
					assertThat(context).doesNotHaveBean("w3cTextMapPropagatorWithoutBaggage");
				});
	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomConfiguration {

		@Bean
		OtelTracer customOtelTracer() {
			return mock(OtelTracer.class);
		}

		@Bean
		EventPublisher customEventPublisher() {
			return mock(EventPublisher.class);
		}

		@Bean
		OtelCurrentTraceContext customOtelCurrentTraceContext() {
			return mock(OtelCurrentTraceContext.class);
		}

		@Bean
		OtelHttpClientHandler customOtelHttpClientHandler() {
			return mock(OtelHttpClientHandler.class);
		}

		@Bean
		OtelHttpServerHandler customOtelHttpServerHandler() {
			return mock(OtelHttpServerHandler.class);
		}

		@Bean
		OpenTelemetry customOpenTelemetry() {
			return mock(OpenTelemetry.class);
		}

		@Bean
		SdkTracerProvider customSdkTracerProvider() {
			return SdkTracerProvider.builder().build();
		}

		@Bean
		ContextPropagators customContextPropagators() {
			return mock(ContextPropagators.class);
		}

		@Bean
		Sampler customSampler() {
			return mock(Sampler.class);
		}

		@Bean
		SpanProcessor customSpanProcessor() {
			return mock(SpanProcessor.class);
		}

		@Bean
		Tracer customTracer() {
			return mock(Tracer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class OpenTelemetryConfiguration {

		@Bean
		OpenTelemetry openTelemetry() {
			return mock(OpenTelemetry.class, Answers.RETURNS_MOCKS);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class ContextPropagatorsConfiguration {

		@Bean
		ContextPropagators contextPropagators() {
			return mock(ContextPropagators.class, Answers.RETURNS_MOCKS);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomFactoryConfiguration {

		@Bean
		TextMapPropagator customPropagationFactory() {
			return mock(TextMapPropagator.class);
		}

	}

}
