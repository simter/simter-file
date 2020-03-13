package tech.simter.file.impl.dao.r2dbc

import org.springframework.data.r2dbc.core.DatabaseClient
import tech.simter.file.TABLE_FILE
import tech.simter.file.core.FileStore
import tech.simter.file.standardModuleValue
import tech.simter.file.test.TestHelper.randomFileStore

object TestHelper {
  /** delete all file store data from database */
  fun clean(client: DatabaseClient) {
    client.delete()
      .from(TABLE_FILE)
      .fetch()
      .rowsUpdated()
      .block()!!
  }

  /** insert one file store instance to database */
  fun insert(
    client: DatabaseClient,
    file: FileStore = randomFileStore()
  ): FileStore {
    return client.insert()
      .into(TABLE_FILE)
      .value("id", file.id)
      .value("module", standardModuleValue(file.module))
      .value("name", file.name)
      .value("type", file.type)
      .value("size", file.size)
      .value("path", file.path)
      .value("creator", file.creator)
      .value("create_on", file.createOn)
      .value("modifier", file.modifier)
      .value("modify_on", file.modifyOn)
      .fetch()
      .rowsUpdated()
      .map { file }
      .block()!!
  }
}