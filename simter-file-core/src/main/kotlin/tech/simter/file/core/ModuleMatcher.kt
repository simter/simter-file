package tech.simter.file.core

import tech.simter.file.standardModuleValue

/**
 * A module matcher.
 *
 * @author RJ
 */
sealed class ModuleMatcher(open val module: String) {
  data class ModuleEquals(override val module: String) : ModuleMatcher(module)
  data class ModuleStartsWith(override val module: String) : ModuleMatcher(module)

  companion object {
    /** Return [ModuleStartsWith] if [module] ends with '%' otherwise return [ModuleEquals] */
    fun autoModuleMatcher(module: String): ModuleMatcher {
      val value = standardModuleValue(module)
      return if (value.endsWith("/")) ModuleEquals(module)
      else ModuleStartsWith(module)
    }
  }
}