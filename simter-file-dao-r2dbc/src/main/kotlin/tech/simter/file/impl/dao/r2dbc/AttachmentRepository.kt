package tech.simter.file.impl.dao.r2dbc

import org.springframework.data.r2dbc.repository.R2dbcRepository
import tech.simter.file.impl.dao.r2dbc.po.AttachmentPo

/**
 * The reactive repository.
 *
 * @author RJ
 */
interface AttachmentRepository : R2dbcRepository<AttachmentPo, String>