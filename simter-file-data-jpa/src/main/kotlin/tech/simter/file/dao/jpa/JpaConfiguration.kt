package tech.simter.file.dao.jpa

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.util.concurrent.Executors


/**
 * The JPA configuration.
 *
 * @author RJ
 */
@Configuration
@EnableJpaRepositories("tech.simter")
@EntityScan(basePackages = ["tech.simter"])
class JpaConfiguration(@Value("\${spring.datasource.maximum-pool-size:1}") val connectionPoolSize: Int) {
  @Bean
  fun jpaScheduler(): Scheduler {
    return Schedulers.fromExecutor(Executors.newFixedThreadPool(connectionPoolSize))
  }
}