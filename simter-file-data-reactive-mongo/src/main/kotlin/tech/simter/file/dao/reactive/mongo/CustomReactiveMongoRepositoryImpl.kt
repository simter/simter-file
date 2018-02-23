package tech.simter.file.dao.reactive.mongo

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository
import reactor.core.publisher.Mono
import java.io.Serializable
import java.util.stream.Collectors

/**
 * The mongodb implementation of [SimpleReactiveMongoRepository] by custom.
 *
 * @author cjw
 */
class CustomReactiveMongoRepositoryImpl<T, ID : Serializable> @Autowired constructor(
  private val entityInformation: MongoEntityInformation<T, ID>,
  private val operations: ReactiveMongoOperations
) : SimpleReactiveMongoRepository<T, ID>(entityInformation, operations) {

  fun findAll(pageable: Pageable): Mono<Page<T>> {
    val query = Query().with(pageable)
    val entities = operations.find(query, entityInformation.javaType, entityInformation.collectionName)
      .toStream().collect(Collectors.toList())
    val total = operations.count(query, entityInformation.javaType, entityInformation.collectionName)
      .blockOptional().orElse(0)
    return Mono.justOrEmpty<Page<T>>(PageImpl<T>(entities, pageable, total))
  }
}