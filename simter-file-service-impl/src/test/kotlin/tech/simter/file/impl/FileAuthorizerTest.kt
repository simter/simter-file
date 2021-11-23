package tech.simter.file.impl

import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Mono
import reactor.kotlin.test.test
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.OPERATION_CREATE
import tech.simter.file.OPERATION_READ
import tech.simter.reactive.security.ReactiveSecurityService
import tech.simter.util.RandomUtils.randomString

/**
 * Test [FileAuthorizer].
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class FileAuthorizerTest @Autowired constructor(
  private val securityService: ReactiveSecurityService,
  private val fileAuthorizer: FileAuthorizer
) {
  // allow
  @Test
  fun `module-has-permission-true and regardless default`() {
    val module = "module-a"

    // set mock
    every { securityService.hasAnyRole("X_READ1", "X_READ2") } returns Mono.just(true)

    // reactive check
    fileAuthorizer.hasPermission(module, OPERATION_READ)
      .test().expectNext(true).verifyComplete()

    // check mock
    verify(exactly = 1) {
      securityService.hasAnyRole("X_READ1", "X_READ2")
    }
    verify(exactly = 0) {
      securityService.hasAnyRole("ADMIN")
    }
  }

  // allow
  @Test
  fun `module-has-permission-false but default-has-permission-true`() {
    val module = "module-a"

    // set mock
    every { securityService.hasAnyRole("X_READ1", "X_READ2") } returns Mono.just(false)
    every { securityService.hasAnyRole("ADMIN") } returns Mono.just(true)

    // reactive check
    fileAuthorizer.hasPermission(module, OPERATION_READ)
      .test().expectNext(true).verifyComplete()

    // check mock
    verify(exactly = 1) {
      securityService.hasAnyRole("X_READ1", "X_READ2")
      securityService.hasAnyRole("ADMIN")
    }
  }

  // deny
  @Test
  fun `module-has-permission-false and default-has-permission-false`() {
    val module = "module-a"

    // set mock
    every { securityService.hasAnyRole("X_READ1", "X_READ2") } returns Mono.just(false)
    every { securityService.hasAnyRole("ADMIN") } returns Mono.just(false)

    // reactive check
    fileAuthorizer.hasPermission(module, OPERATION_READ)
      .test().expectNext(false).verifyComplete()

    // check mock
    verify(exactly = 1) {
      securityService.hasAnyRole("X_READ1", "X_READ2")
      securityService.hasAnyRole("ADMIN")
    }
  }

  // allow
  @Test
  fun `sub-module-has-permission-true and regardless default`() {
    val module = "module-a/sub-module"

    // set mock
    every { securityService.hasAnyRole("X_READ1", "X_READ2") } returns Mono.just(true)

    // reactive check
    fileAuthorizer.hasPermission(module, OPERATION_READ)
      .test().expectNext(true).verifyComplete()

    // check mock
    verify(exactly = 1) {
      securityService.hasAnyRole("X_READ1", "X_READ2")
    }
    verify(exactly = 0) {
      securityService.hasAnyRole("ADMIN")
    }
  }

  // allow
  @Test
  fun `module-verify-permission-success and regardless default`() {
    val module = "module-a"

    // set mock
    every { securityService.verifyHasAllRole("X_CREATE") } returns Mono.empty()

    // reactive check
    fileAuthorizer.verifyHasPermission(module, OPERATION_CREATE)
      .test().verifyComplete()

    // check mock
    verify(exactly = 1) {
      securityService.verifyHasAllRole("X_CREATE") // strategy and
    }
    verify(exactly = 0) {
      securityService.verifyHasAnyRole("ADMIN")    // strategy or
    }
  }

  // allow
  @Test
  fun `module-verify-permission-failed but default-verify-permission-success`() {
    val module = "module-a"

    // set mock
    every { securityService.verifyHasAllRole("X_CREATE") } returns Mono.error(PermissionDeniedException())
    every { securityService.verifyHasAnyRole("ADMIN") } returns Mono.empty()

    // reactive check
    fileAuthorizer.verifyHasPermission(module, OPERATION_CREATE)
      .test().verifyComplete()

    // check mock
    verify(exactly = 1) {
      securityService.verifyHasAllRole("X_CREATE")
      securityService.verifyHasAnyRole("ADMIN")
    }
  }

  // deny
  @Test
  fun `module-verify-permission-failed and default-verify-permission-failed`() {
    val module = "module-a"

    // set mock
    every { securityService.verifyHasAllRole("X_CREATE") } returns Mono.error(PermissionDeniedException())
    every { securityService.verifyHasAnyRole("ADMIN") } returns Mono.error(PermissionDeniedException())

    // reactive check
    fileAuthorizer.verifyHasPermission(module, OPERATION_CREATE)
      .test()
      .expectErrorSatisfies {
        assertThat(it).isExactlyInstanceOf(PermissionDeniedException::class.java)
          .hasMessage("Permission denied on super $OPERATION_CREATE")
      }.verify()

    // check mock
    verify(exactly = 1) {
      securityService.verifyHasAllRole("X_CREATE")
      securityService.verifyHasAnyRole("ADMIN")
    }
  }

  // allow
  @Test
  fun `no-module-cfg and default-has-permission-true`() {
    val module = randomString()

    // set mock
    every { securityService.hasAnyRole("ADMIN") } returns Mono.just(true)

    // reactive check
    fileAuthorizer.hasPermission(module, OPERATION_READ)
      .test().expectNext(true).verifyComplete()

    // check mock
    verify(exactly = 1) {
      securityService.hasAnyRole("ADMIN")
    }
    verify(exactly = 0) {
      securityService.hasAnyRole(*varargAny { it != "ADMIN" })
      securityService.hasAllRole(*varargAny { it != "ADMIN" })
    }
  }

  // deny
  @Test
  fun `no-module-cfg and default-has-permission-false`() {
    val module = randomString()

    // set mock
    every { securityService.hasAnyRole("ADMIN") } returns Mono.just(false)

    // reactive check
    fileAuthorizer.hasPermission(module, OPERATION_READ)
      .test().expectNext(false).verifyComplete()

    // check mock
    verify(exactly = 1) {
      securityService.hasAnyRole("ADMIN")
    }
    verify(exactly = 0) {
      securityService.hasAnyRole(*varargAny { it != "ADMIN" })
      securityService.hasAllRole(*varargAny { it != "ADMIN" })
    }
  }

  // allow
  @Test
  fun `no-module-cfg and default-verify-permission-success`() {
    val module = randomString()

    // set mock
    every { securityService.verifyHasAnyRole("ADMIN") } returns Mono.empty()

    // reactive check
    fileAuthorizer.verifyHasPermission(module, OPERATION_READ)
      .test().verifyComplete()

    // check mock
    verify(exactly = 1) {
      securityService.verifyHasAnyRole("ADMIN")
    }
    verify(exactly = 0) {
      securityService.verifyHasAnyRole(*varargAny { it != "ADMIN" })
      securityService.verifyHasAllRole(*varargAny { it != "ADMIN" })
    }
  }

  // deny
  @Test
  fun `no-module-cfg and default-verify-permission-failed`() {
    val module = randomString()

    // set mock
    every { securityService.verifyHasAnyRole("ADMIN") } returns Mono.error(PermissionDeniedException())

    // reactive check
    fileAuthorizer.verifyHasPermission(module, OPERATION_READ)
      .test()
      .expectErrorSatisfies {
        assertThat(it).isExactlyInstanceOf(PermissionDeniedException::class.java)
          .hasMessage("Permission denied on super $OPERATION_READ")
      }.verify()

    // check mock
    verify(exactly = 1) {
      securityService.verifyHasAnyRole("ADMIN")
    }
    verify(exactly = 0) {
      securityService.verifyHasAnyRole(*varargAny { it != "ADMIN" })
      securityService.verifyHasAllRole(*varargAny { it != "ADMIN" })
    }
  }
}