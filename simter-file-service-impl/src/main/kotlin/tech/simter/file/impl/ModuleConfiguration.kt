package tech.simter.file.impl

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import tech.simter.file.DEFAULT_MODULE_AUTHORIZER_KEY
import tech.simter.file.PACKAGE
import tech.simter.file.MODULES_AUTHORIZER_KEY
import tech.simter.reactive.security.ModuleAuthorizer
import tech.simter.reactive.security.ReactiveSecurityService
import tech.simter.reactive.security.properties.ModuleAuthorizeProperties
import tech.simter.reactive.security.properties.PermissionStrategy.Allow

/**
 * All configuration for this module.
 *
 * @author zh
 * @author RJ
 */
@Configuration("$PACKAGE.impl.ModuleConfiguration")
@EnableConfigurationProperties
@ComponentScan
class ModuleConfiguration {
  /**
   * Starter should config a yml key [DEFAULT_MODULE_AUTHORIZER_KEY] to support default module access-control,
   * otherwise the [ModuleConfiguration.defaultModuleAuthorizer] would allow anything default.
   *
   * Mostly this is for admin-module control.
   */
  @Bean("$DEFAULT_MODULE_AUTHORIZER_KEY.properties")
  @ConfigurationProperties(prefix = DEFAULT_MODULE_AUTHORIZER_KEY)
  fun defaultModuleAuthorizeProperties(): ModuleAuthorizeProperties {
    return ModuleAuthorizeProperties(defaultPermission = Allow)
  }

  @Bean("$DEFAULT_MODULE_AUTHORIZER_KEY.authorizer")
  fun defaultModuleAuthorizer(
    @Qualifier("$DEFAULT_MODULE_AUTHORIZER_KEY.properties")
    properties: ModuleAuthorizeProperties,
    securityService: ReactiveSecurityService
  ): ModuleAuthorizer {
    return ModuleAuthorizer.create(properties, securityService)
  }

  /**
   * Starter could config multiple yml key [MODULES_AUTHORIZER_KEY] to
   * support specific business module access-control when it is necessary,
   * otherwise all business modules access-control would fall back to use [ModuleConfiguration.defaultModuleAuthorizer].
   */
  @Bean("$MODULES_AUTHORIZER_KEY.properties")
  @ConfigurationProperties(prefix = MODULES_AUTHORIZER_KEY)
  fun modulesAuthorizeProperties(): Map<String, ModuleAuthorizeProperties> {
    return HashMap()
  }

  @Bean("$MODULES_AUTHORIZER_KEY.authorizers")
  fun modulesAuthorizers(
    @Qualifier("$MODULES_AUTHORIZER_KEY.properties")
    properties: Map<String, ModuleAuthorizeProperties>,
    securityService: ReactiveSecurityService
  ): Map<String, ModuleAuthorizer> {
    return properties.mapValues { ModuleAuthorizer.create(it.value, securityService) }
  }
}