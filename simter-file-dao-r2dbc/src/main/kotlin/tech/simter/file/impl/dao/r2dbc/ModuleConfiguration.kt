package tech.simter.file.impl.dao.r2dbc

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import tech.simter.file.PACKAGE
import tech.simter.file.core.FileStore
import tech.simter.file.impl.dao.r2dbc.po.FileStorePo

/**
 * All configuration for this module.
 *
 * @author RJ
 */
@Configuration("$PACKAGE.impl.dao.r2dbc.ModuleConfiguration")
@EnableR2dbcRepositories
@ComponentScan
class ModuleConfiguration {
  @Bean
  fun serializersModule4FileStorePo(): SerializersModule {
    return SerializersModule {
      polymorphic(FileStore::class) {
        subclass(FileStorePo::class)
      }

      // for Page<T>
      // https://github.com/simter/simter-kotlin/blob/master/src/test/kotlin/tech/simter/kotlin/serialization/PageTest.kt
      // https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#polymorphism-and-generic-classes
      polymorphic(Any::class) { subclass(FileStorePo::class) }
    }
  }
}