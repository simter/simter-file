package tech.simter.file.data.reactive.mongo

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.mapping.event.LoggingEventListener
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

/**
 * The reactive MongoDB configuration.
 *
 * @author RJ
 */
@Configuration
@EnableReactiveMongoRepositories("tech.simter")
class MongoConfiguration {
  @Bean
  @ConditionalOnProperty(name = ["simter.mongodb.logging-event-listener"], havingValue = "true")
  fun mongoEventListener(): LoggingEventListener {
    return LoggingEventListener()
  }
}