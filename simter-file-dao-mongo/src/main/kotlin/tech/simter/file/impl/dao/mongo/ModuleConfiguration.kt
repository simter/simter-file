package tech.simter.file.impl.dao.mongo

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import tech.simter.file.PACKAGE

/**
 * All configuration for this module.
 *
 * @author RJ
 */
@Configuration("$PACKAGE.impl.dao.mongo.ModuleConfiguration")
@EnableReactiveMongoRepositories
@ComponentScan
@Import(tech.simter.mongo.ModuleConfiguration::class) // auto register converters
@EntityScan("$PACKAGE.impl.dao.mongo.po")
class ModuleConfiguration