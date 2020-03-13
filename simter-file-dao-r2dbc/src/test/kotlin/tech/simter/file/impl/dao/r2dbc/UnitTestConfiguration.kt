package tech.simter.file.impl.dao.r2dbc

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * All configuration for this module.
 *
 * @author RJ
 */
@Configuration
@Import(ModuleConfiguration::class)
@ComponentScan("tech.simter")
class UnitTestConfiguration