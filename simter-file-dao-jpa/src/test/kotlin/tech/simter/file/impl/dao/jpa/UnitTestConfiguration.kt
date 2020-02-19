package tech.simter.file.impl.dao.jpa

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * @author RJ
 */
@Configuration
@ComponentScan("tech.simter")
@Import(ModuleConfiguration::class)
class UnitTestConfiguration