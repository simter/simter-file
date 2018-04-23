package tech.simter.file.dao.reactive.mongo

import org.bson.Document
import org.springframework.core.convert.converter.Converter
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * See [Data mapping and type conversion](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/#mapping-conversion)
 * @author cjw
 */
class DocumentToOffsetDateTimeConverter : Converter<Document, OffsetDateTime> {
  override fun convert(source: Document): OffsetDateTime {
    var dateTime = source.getDate("dateTime").toInstant()
    var timeZone = ZoneOffset.of(source.getString("offset"))
    return OffsetDateTime.ofInstant(dateTime, timeZone)
  }
}