package tech.simter.file.impl.dao.r2dbc

import org.springframework.r2dbc.core.DatabaseClient
import tech.simter.file.TABLE_FILE
import tech.simter.file.core.FileStore
import tech.simter.file.standardModuleValue
import tech.simter.file.test.TestHelper.randomFileStore

object TestHelper {
  /** delete all file store data from database */
  fun clean(client: DatabaseClient) {
    client.sql("delete from $TABLE_FILE").then().block()
  }

  /** insert one file store instance to database */
  fun insert(
    client: DatabaseClient,
    file: FileStore = randomFileStore()
  ): FileStore {
    return client.sql("""
      insert into $TABLE_FILE (
        id, module, name, type, size, path, creator, create_on, modifier, modify_on
      ) values (
        :id, :module, :name, :type, :size, :path, :creator, :createOn, :modifier, :modifyOn
      )
    """.trimIndent())
      .bind("module", standardModuleValue(file.module))
      .bind("name", file.name)
      .bind("type", file.type)
      .bind("size", file.size)
      .bind("path", file.path)
      .bind("creator", file.creator)
      .bind("createOn", file.createOn)
      .bind("modifier", file.modifier)
      .bind("modifyOn", file.modifyOn)
      .bind("id", if (file.id.isNotEmpty()) file.id else TODO("Not implemented for auto generate id by database"))
      .fetch()
      .rowsUpdated()
      .map { file }
      .block()!!
  }
}