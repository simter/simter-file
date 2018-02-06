package tech.simter.file.data.reactive.mongo

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.CustomConversions
import org.springframework.data.mongodb.config.MongoConfigurationSupport
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
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
@EnableReactiveMongoRepositories("tech.simter")
class MongoConfiguration : MongoConfigurationSupport() {
  @Bean
  @ConditionalOnProperty(name = ["simter.mongodb.logging-event-listener"], havingValue = "true")
  fun mongoEventListener(): LoggingEventListener {
    return LoggingEventListener()
  }

  override fun getDatabaseName(): String {
    return "test"
  }

  /**
   * Add converters to custom conversions list
   */
  override fun customConversions(): CustomConversions {
    var converterList = ArrayList<Converter<*, *>>()
    converterList.add(MongoDocumentToOffsetDateTimeConverter())
    return MongoCustomConversions(converterList)
  }

  /**
   * Registering the converters
   */
  @Bean
  @Throws(Exception::class)
  fun mappingMongoConverter(): MappingMongoConverter {
    val converter = MappingMongoConverter(ReactiveMongoTemplate.NO_OP_REF_RESOLVER,
      mongoMappingContext())
    converter.setCustomConversions(customConversions())
    return converter
  }
}