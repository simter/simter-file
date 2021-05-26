package tech.simter.file.impl.dao.jpa

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import tech.simter.file.PACKAGE
import tech.simter.file.core.FileStore
import tech.simter.file.impl.dao.jpa.po.FileStorePo

/**
 * All configuration for this module.
 *
 * @author RJ
 */
@Configuration("$PACKAGE.impl.dao.jpa.ModuleConfiguration")
@ComponentScan
@EnableJpaRepositories
@EntityScan("$PACKAGE.impl.dao.jpa.po")
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