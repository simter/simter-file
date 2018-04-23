package tech.simter.file.dao.reactive.mongo

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.convert.CustomConversions
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.mapping.event.LoggingEventListener
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

private const val MODULE_PACKAGE = "tech.simter.file.dao.reactive.mongo"

/**
 * All configuration for this module.
 *
 * @author RJ
 */
@Configuration("tech.simter.file.dao.reactive.mongo.ModuleConfiguration")
@EnableReactiveMongoRepositories(MODULE_PACKAGE)
@ComponentScan(MODULE_PACKAGE)
@EntityScan(basePackages = ["tech.simter.file.po"])
class ModuleConfiguration {
  private val logger = LoggerFactory.getLogger(ModuleConfiguration::class.java)
  @Bean
  @ConditionalOnProperty(name = ["simter.mongodb.enabled-logging-event-listener"], havingValue = "true")
  @ConditionalOnMissingBean
  fun mongoEventListener(): LoggingEventListener {
    logger.warn("instance a LoggingEventListener bean for mongodb operations")
    return LoggingEventListener()
  }

  /**
   * Add converters to custom conversions list
   */
  @Primary
  @Bean("tech.simter.file.dao.reactive.mongo.CustomConversions")
  @ConditionalOnMissingBean(name = ["tech.simter.file.dao.reactive.mongo.CustomConversions"])
  fun customConversions(): CustomConversions {
    return MongoCustomConversions(listOf(DocumentToOffsetDateTimeConverter()))
  }
}