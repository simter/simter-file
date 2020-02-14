package tech.simter.file.service

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.simter.kotlin.properties.AuthorizeModuleOperations

private const val MODULE = "tech.simter.file"

/**
 * All configuration for this module.
 *
 * @author zh
 */
@Configuration("$MODULE.dao.service.ModuleConfiguration")
@EnableConfigurationProperties
class ModuleConfiguration {
  @Bean
  @ConfigurationProperties(prefix = "module.authorization.simter-file")
  fun authorizeModuleOperations(): AuthorizeModuleOperations {
    return AuthorizeModuleOperations()
  }
}