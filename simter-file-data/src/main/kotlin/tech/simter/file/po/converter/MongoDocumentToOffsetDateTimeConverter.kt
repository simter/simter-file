package tech.simter.file.po.converter

import org.bson.Document
import org.springframework.core.convert.converter.Converter
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * @author cjw
 */
class MongoDocumentToOffsetDateTimeConverter : Converter<Document, OffsetDateTime> {
  override fun convert(source: Document): OffsetDateTime {
    var dateTime = source.getDate("dateTime").toInstant()
    var timeZone = ZoneOffset.of(source.getString("offset"))
    return OffsetDateTime.ofInstant(dateTime, timeZone)
  }
}