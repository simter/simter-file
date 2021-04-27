package tech.simter.file.rest.webflux

import com.ninjasquad.springmockk.MockkBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunction
import tech.simter.file.core.FileService

/**
 * All unit test config for this module.
 *
 * This config will auto register a [WebTestClient] instance base on spring bean config,
 * and could be used for injection.
 *
 * Because [WebTestClient] requires [RouterFunction], so need to register a [RouterFunction]
 * on your unit test class. Such as:
 *
 * ```
 * @SpringJUnitConfig(UnitTestConfiguration::class, MyHandler::class)
 * @WebFluxTest
 * class MyHandlerTest @Autowired constructor(
 *   private val client: WebTestClient,
 *   private val myService: MyService
 * ) {
 *   @Configuration
 *   class Cfg {
 *     @Bean
 *     fun theRoute(handler: MyHandler): RouterFunction<ServerResponse> = route(REQUEST_PREDICATE, handler)
 *   }
 *
 *   @Test
 *   fun testMethod() {
 *     ...
 *   }
 * }
 * ```
 *
 * @author RJ
 */
@Configuration
@EnableWebFlux
@ComponentScan("tech.simter", "cn.gftaxi")
@MockkBean(FileService::class)
class UnitTestConfiguration