package tech.simter.file.dao.reactive.mongo

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.CustomConversions
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.mapping.event.LoggingEventListener
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import tech.simter.file.po.converter.MongoDocumentToOffsetDateTimeConverter
import java.util.*


/**
 * The reactive MongoDB configuration.
 *
 * @author RJ
 */
@Configuration
@EnableReactiveMongoRepositories(value = "tech.simter", repositoryBaseClass = CustomReactiveMongoRepositoryImpl::class)
class MongoConfiguration {
  @Bean
  @ConditionalOnProperty(name = ["simter.mongodb.logging-event-listener"], havingValue = "true")
  fun mongoEventListener(): LoggingEventListener {
    return LoggingEventListener()
  }

  /**
   * Add converters to custom conversions list
   */
  @Bean
  @Primary
  fun customConversions(): CustomConversions {
    var converterList = ArrayList<Converter<*, *>>()
    converterList.add(MongoDocumentToOffsetDateTimeConverter())
    return MongoCustomConversions(converterList)
  }
}