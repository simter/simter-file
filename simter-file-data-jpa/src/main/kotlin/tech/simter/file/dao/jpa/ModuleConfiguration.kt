package tech.simter.file.dao.jpa

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

private const val MODULE_PACKAGE = "tech.simter.file.dao.jpa"

/**
 * All configuration for this module.
 *
 * @author RJ
 */
@Configuration("$MODULE_PACKAGE.ModuleConfiguration")
@ComponentScan(MODULE_PACKAGE)
@EnableJpaRepositories(MODULE_PACKAGE)
@EntityScan(basePackages = ["tech.simter.file.po"])
class ModuleConfiguration