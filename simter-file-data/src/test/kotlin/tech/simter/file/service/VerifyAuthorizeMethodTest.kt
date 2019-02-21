package tech.simter.file.service

import com.nhaarman.mockito_kotlin.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.service.AttachmentServiceImpl.OperationType
import tech.simter.file.service.AttachmentServiceImpl.OperationType.Delete
import tech.simter.file.service.AttachmentServiceImpl.OperationType.Read
import tech.simter.kotlin.properties.AuthorizeModuleOperations
import tech.simter.reactive.security.ReactiveSecurityService

/**
 * Test [AttachmentServiceImpl.verifyAuthorize]
 *
 * @author zh
 */
@SpringBootTest(classes = [VerifyAuthorizeMethodTest.Cfg::class])
@MockBean(AttachmentDao::class, ReactiveSecurityService::class)
class VerifyAuthorizeMethodTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val securityService: ReactiveSecurityService,
  @Qualifier("nothing") private val nothing: AuthorizeModuleOperations,
  @Qualifier("onlyModule") private val onlyModule: AuthorizeModuleOperations,
  @Qualifier("hasBasic") private val hasBasic: AuthorizeModuleOperations
) {
  @Configuration
  @EnableConfigurationProperties
  class Cfg {
    @Bean(name = ["nothing"])
    @ConfigurationProperties(prefix = "module.authorization.simter-file.test-nothing")
    fun authorizeModuleOperationsNothing(): AuthorizeModuleOperations {
      return AuthorizeModuleOperations()
    }

    @Bean(name = ["onlyModule"])
    @ConfigurationProperties(prefix = "module.authorization.simter-file.test-only-module")
    fun authorizeModuleOperationsNoDefault(): AuthorizeModuleOperations {
      return AuthorizeModuleOperations()
    }

    @Bean(name = ["hasBasic"])
    @ConfigurationProperties(prefix = "module.authorization.simter-file.test-has-basic")
    fun authorizeModuleOperationsHasDefault(): AuthorizeModuleOperations {
      return AuthorizeModuleOperations()
    }
  }

  @Test
  fun `Nothing properties`() {
    // mock
    val service = AttachmentServiceImpl("", dao, nothing, securityService)

    // invoke and verify
    AttachmentServiceImpl.OperationType.values().forEach {
      StepVerifier.create(service.verifyAuthorize(null, it)).verifyComplete()
      StepVerifier.create(service.verifyAuthorize("TEST_MODULE", it)).verifyComplete()
      StepVerifier.create(service.verifyAuthorize("admin", it)).verifyError()
    }
    verify(securityService, times(0)).verifyHasAnyRole(anyOrNull(), any())
    verify(securityService, times(0)).verifyHasAllRole(anyOrNull(), any())
  }

  @Test
  fun `Test only module properties`() {
    // mock
    val service = AttachmentServiceImpl("", dao, onlyModule, securityService)
    val error = Mono.error<Void>(PermissionDeniedException())
    `when`(securityService.verifyHasAnyRole(anyVararg())).thenReturn(error)
    `when`(securityService.verifyHasAllRole(anyVararg())).thenReturn(error)

    // invoke and verify
    OperationType.values().forEach {
      if (it == Delete) StepVerifier.create(service.verifyAuthorize("TEST_MODULE_A", it)).verifyComplete()
      else StepVerifier.create(service.verifyAuthorize("TEST_MODULE_A", it)).verifyError()
      StepVerifier.create(service.verifyAuthorize("TEST_MODULE_B", it)).verifyComplete()
      StepVerifier.create(service.verifyAuthorize("admin", it)).verifyError()
    }
    verify(securityService, times(1)).verifyHasAnyRole(anyVararg())
    verify(securityService, times(2)).verifyHasAllRole(anyVararg())
    verify(securityService, times(1)).verifyHasAnyRole("moduleA-R-1", "moduleA-R-2")
    verify(securityService, times(2)).verifyHasAllRole("moduleA-CU-1", "moduleA-CU-2")
  }

  @Test
  fun `Test has basic properties`() {
    // mock
    val service = AttachmentServiceImpl("", dao, hasBasic, securityService)
    val error = Mono.error<Void>(PermissionDeniedException())
    `when`(securityService.verifyHasAnyRole(anyVararg())).thenReturn(error)
    `when`(securityService.verifyHasAllRole(anyVararg())).thenReturn(error)

    // invoke and verify
    OperationType.values().forEach {
      // denied TEST_MODULE_A but pass admin
      if (it == Delete) StepVerifier.create(service.verifyAuthorize("TEST_MODULE_A", it)).verifyComplete()
      // denied TEST_MODULE_A and denied admin
      else StepVerifier.create(service.verifyAuthorize("TEST_MODULE_A", it)).verifyError()
      // denied default and denied admin
      if (it == Read) StepVerifier.create(service.verifyAuthorize("TEST_MODULE_B", it)).verifyError()
      // pass default
      else StepVerifier.create(service.verifyAuthorize("TEST_MODULE_B", it)).verifyComplete()
      // pass admin
      if (it == Delete) StepVerifier.create(service.verifyAuthorize("admin", it)).verifyComplete()
      // denied admin
      else StepVerifier.create(service.verifyAuthorize("admin", it)).verifyError()
    }
    verify(securityService, times(5)).verifyHasAnyRole(anyVararg())
    verify(securityService, times(7)).verifyHasAllRole(anyVararg())
    verify(securityService, times(1)).verifyHasAnyRole("moduleA-R-1", "moduleA-R-2")
    verify(securityService, times(2)).verifyHasAllRole("moduleA-CU-1", "moduleA-CU-2")
    verify(securityService, times(1)).verifyHasAllRole("moduleA-D-1", "moduleA-D-2")
    verify(securityService, times(1)).verifyHasAnyRole("default-R-1", "default-R-2")
    verify(securityService, times(3)).verifyHasAnyRole("admin-R-1", "admin-R-2")
    verify(securityService, times(4)).verifyHasAllRole("admin-CU-1", "admin-CU-2")
  }
}