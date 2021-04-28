package tech.simter.file

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.simter.file.core.FileStore

/**
 * All configuration for this module.
 *
 * @author RJ
 */
@Configuration("$PACKAGE.core")
class ModuleConfiguration {
  @Bean
  fun serializersModule4FileStore(): SerializersModule {
    return SerializersModule {
      polymorphic(FileStore::class) {
        subclass(FileStore.Impl::class)
      }

      // for Page<T>
      // https://github.com/simter/simter-kotlin/blob/master/src/test/kotlin/tech/simter/kotlin/serialization/PageTest.kt
      // https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#polymorphism-and-generic-classes
      polymorphic(Any::class) { subclass(FileStore.Impl::class) }
    }
  }
}