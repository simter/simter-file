package tech.simter.file.impl.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import tech.simter.file.DEFAULT_MODULE_AUTHORIZER_KEY
import tech.simter.file.PACKAGE
import tech.simter.file.SUB_MODULES_AUTHORIZER_KEY
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
   * Starter could config multiple yml key [SUB_MODULES_AUTHORIZER_KEY] to
   * support specific sub-module access-control when it is necessary,
   * otherwise all sub-module access-control would fallback to use [ModuleConfiguration.defaultModuleAuthorizer].
   */
  @Bean("$SUB_MODULES_AUTHORIZER_KEY.properties")
  @ConfigurationProperties(prefix = SUB_MODULES_AUTHORIZER_KEY)
  fun subModuleAuthorizePropertiesMap(): Map<String, ModuleAuthorizeProperties> {
    return HashMap()
  }

  @Bean("$SUB_MODULES_AUTHORIZER_KEY.authorizer")
  fun subModuleAuthorizerMap(
    @Qualifier("$SUB_MODULES_AUTHORIZER_KEY.properties")
    properties: Map<String, ModuleAuthorizeProperties>,
    securityService: ReactiveSecurityService
  ): Map<String, ModuleAuthorizer> {
    return properties.mapValues { ModuleAuthorizer.create(it.value, securityService) }
  }
}