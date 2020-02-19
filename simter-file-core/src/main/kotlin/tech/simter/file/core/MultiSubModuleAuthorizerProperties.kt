package tech.simter.file.core

import tech.simter.reactive.security.properties.ModuleAuthorizeProperties

/**
 * Multiple SubModule [ModuleAuthorizeProperties] config class.
 *
 * Sample :
 *
 * ```
 * module.authorization.simter-file:
 *   sub-modules:
 *     sub-module1:
 *       defaultPermission: "Allow"
 *       operations:
 *         read:
 *           roles: ["X_READ1", "X_READ2"]
 *           strategy: 'or'
 *         create:
 *           roles: ["X_CREAT", "X_UPDATE"]
 *           strategy: "and"
 *         delete:
 *           roles: ["X_DELETE"]
 *     sub-module2: ...
 *     ...
 * ```
 *
 * @author RJ
 * @see [ModuleAuthorizeProperties]
 */
data class MultiSubModuleAuthorizerProperties(
  /** key is the sub-module identity, value is a nested [ModuleAuthorizeProperties] properties config */
  val modules: Map<String, ModuleAuthorizeProperties>? = HashMap()
)