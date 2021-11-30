package tech.simter.file.impl

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import tech.simter.file.DEFAULT_MODULE_AUTHORIZER_KEY
import tech.simter.file.MODULES_AUTHORIZER_KEY
import tech.simter.reactive.security.ModuleAuthorizer

/**
 * The file authorizer.
 *
 * @author RJ
 */
@Component
class FileAuthorizer @Autowired constructor(
  @Qualifier("$DEFAULT_MODULE_AUTHORIZER_KEY.authorizer")
  private val defaultModuleAuthorizer: ModuleAuthorizer,
  @Qualifier("$MODULES_AUTHORIZER_KEY.authorizers")
  private val modulesAuthorizers: Map<String, ModuleAuthorizer>
) {
  private val logger: Logger = LoggerFactory.getLogger(FileAuthorizer::class.java)

  init {
    logger.warn("defaultAuthorizer={}, modulesAuthorizers={}",
      defaultModuleAuthorizer, modulesAuthorizers.keys.joinToString(","))
  }

  /**
   * Determine whether the system-context has permission to do the specific [operation] on the [module].
   *
   * First to check specific module configuration, if failed or without this module configuration,
   * fall back to check default module configuration.
   */
  fun hasPermission(module: String, operation: String): Mono<Boolean> {
    return getModuleAuthorizer(module)?.hasPermission(operation)
      ?.flatMap {
        if (!it) defaultModuleAuthorizer.hasPermission(operation)
        else Mono.just(it)
      }
      ?: defaultModuleAuthorizer.hasPermission(operation)
  }

  /**
   * Verify whether the system-context has permission to do the specific [operation] on the [module].
   *
   * First to check specific module configuration, if failed or without this module configuration,
   * fall back to check default module configuration.
   */
  fun verifyHasPermission(module: String, operation: String): Mono<Void> {
    return getModuleAuthorizer(module)?.verifyHasPermission(operation)
      ?.onErrorResume { defaultModuleAuthorizer.verifyHasPermission(operation) }
      ?: defaultModuleAuthorizer.verifyHasPermission(operation)
  }

  /**
   * Use bellow order to get the match module authorizer:
   *
   * 1. use `module=key` strategy to get matcher from [modulesAuthorizers].
   * 2. use `module-starts-with-key` strategy to get matcher from [modulesAuthorizers].
   * 3. otherwise, return null.
   *
   * if [module] ends with `%`, remove it to do the match.
   */
  private fun getModuleAuthorizer(module: String): ModuleAuthorizer? {
    val m = if (module.endsWith("%")) module.substring(0, module.length - 1) else module
    return modulesAuthorizers[m]
      ?: modulesAuthorizers.entries.firstOrNull { m.startsWith(it.key) }?.value
  }
}