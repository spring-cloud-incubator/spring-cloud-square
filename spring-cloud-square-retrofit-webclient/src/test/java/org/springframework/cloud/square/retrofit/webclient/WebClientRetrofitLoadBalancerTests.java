/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.square.retrofit.webclient;

import java.util.Objects;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Mono;
import retrofit2.Retrofit;
import retrofit2.http.GET;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.square.retrofit.core.RetrofitClient;
import org.springframework.cloud.square.retrofit.core.RetrofitContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest( properties = { "spring.application.name=retrofitclientribbontest",
		"logging.level.org.springframework.cloud.square.retrofit=DEBUG",
		"retrofit.reactor.enabled=false",
		"spring.cloud.loadbalancer.ribbon.enabled=false",
 }, webEnvironment = RANDOM_PORT)
@DirtiesContext
public class WebClientRetrofitLoadBalancerTests {

	protected static final String HELLO_WORLD_1 = "hello world 1";

	@Autowired
	private TestClient testClient;

	@Autowired
	private RetrofitContext retrofitContext;

	@RetrofitClient("localapp")
	protected interface TestClient {
		@GET("/hello")
		Mono<Hello> getHello();
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableRetrofitClients(clients = TestClient.class)
	@LoadBalancerClient(name = "localapp", configuration = TestAppConfig.class)
	@SuppressWarnings("unused")
	@RestController
	protected static class Application {

		@GetMapping("/hello")
		public Mono<Hello> hello() {
			return Mono.just(new Hello(HELLO_WORLD_1));
		}

		@Bean
		@LoadBalanced
		public WebClient.Builder builder() {
			return WebClient.builder();
		}
	}

	@Test
	public void testRetrofitConfiguration() {
		Retrofit retrofit = this.retrofitContext.getInstance("localapp", Retrofit.class);
		assertThat(retrofit).isNotNull();
		assertThat(retrofit.callFactory()).isInstanceOf(WebClientCallFactory.class);
		assertThat(retrofit.callAdapterFactories()).hasAtLeastOneElementOfType(WebClientCallAdapterFactory.class);
		assertThat(retrofit.converterFactories()).hasAtLeastOneElementOfType(WebClientConverterFactory.class);
	}

	@Test
	public void testSimpleType() throws Exception {
		Hello response = this.testClient.getHello().block();
		Assertions.assertThat(response).isEqualTo(new Hello(HELLO_WORLD_1));
	}

	public static class TestAppConfig {
		@LocalServerPort
		private int port = 0;

		@Bean
		public ServiceInstanceListSupplier staticServiceInstanceListSupplier(
				Environment env) {
			return ServiceInstanceListSupplier.fixed(env).instance(port, "local").build();
		}
	}

	private static class Hello {
		private String name;

		public Hello() {
		}

		public Hello(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Hello hello = (Hello) o;
			return Objects.equals(this.name, hello.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.name);
		}

		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("name", name)
					.toString();

		}
	}
}
