package tech.simter.file.impl

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.MockkBeans
import com.ninjasquad.springmockk.SpykBean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import tech.simter.file.DEFAULT_MODULE_AUTHORIZER_KEY
import tech.simter.file.MODULES_AUTHORIZER_KEY
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.service.AttachmentServiceImpl
import tech.simter.file.impl.service.ModuleConfiguration
import tech.simter.reactive.security.ModuleAuthorizer
import tech.simter.reactive.security.ReactiveSecurityService

/**
 * All unit test config for this module.
 *
 * @author RJ
 */
@Configuration
@Import(ModuleConfiguration::class)
@MockkBeans(
  MockkBean(AttachmentDao::class, ReactiveSecurityService::class),
  MockkBean(name = "$DEFAULT_MODULE_AUTHORIZER_KEY.authorizer", classes = [ModuleAuthorizer::class]),
  MockkBean(name = "$MODULES_AUTHORIZER_KEY.authorizers", classes = [ModuleAuthorizer::class])
)
@SpykBean(AttachmentServiceImpl::class)
class UnitTestConfiguration