package tech.simter.file.dao.reactive.mongo

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

private const val MODULE_PACKAGE = "tech.simter.file.dao.reactive.mongo"

/**
 * All configuration for this module.
 *
 * @author RJ
 */
@Import(tech.simter.mongo.ModuleConfiguration::class) // auto register converters
@Configuration("$MODULE_PACKAGE.ModuleConfiguration")
@EnableReactiveMongoRepositories(MODULE_PACKAGE)
@ComponentScan(MODULE_PACKAGE)
@EntityScan(basePackages = ["tech.simter.file.po"])
class ModuleConfiguration