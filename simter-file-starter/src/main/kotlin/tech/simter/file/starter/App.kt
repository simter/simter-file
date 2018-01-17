package tech.simter.file.starter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["tech.simter"])
class App

fun main(args: Array<String>) {
  runApplication<App>(*args)
}