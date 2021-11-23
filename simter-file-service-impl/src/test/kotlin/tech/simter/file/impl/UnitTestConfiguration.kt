package tech.simter.file.impl

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.MockkBeans
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import tech.simter.file.core.FileDao
import tech.simter.reactive.security.ReactiveSecurityService

/**
 * All unit test config for this module.
 *
 * @author RJ
 */
@Configuration
@Import(ModuleConfiguration::class)
@MockkBeans(
  MockkBean(FileDao::class, ReactiveSecurityService::class)
)
class UnitTestConfiguration