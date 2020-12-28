package tech.simter.file.impl.dao.r2dbc

import org.springframework.data.r2dbc.repository.R2dbcRepository
import tech.simter.file.impl.dao.r2dbc.po.FileStorePo

/**
 * The reactive repository.
 *
 * @author RJ
 */
interface FileStoreRepository : R2dbcRepository<FileStorePo, String>