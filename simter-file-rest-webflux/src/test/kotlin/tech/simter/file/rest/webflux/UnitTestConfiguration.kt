package tech.simter.file.rest.webflux

import com.ninjasquad.springmockk.MockkBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.EnableWebFlux
import tech.simter.file.core.FileService

/**
 * All unit test config for this module.
 *
 * @author RJ
 */
@Configuration
@EnableWebFlux
@ComponentScan("tech.simter")
@MockkBean(FileService::class)
class UnitTestConfiguration