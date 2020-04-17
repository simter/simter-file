package tech.simter.file

import tech.simter.file.core.FilePack
import tech.simter.kotlin.serialization.JsonUtils
import java.io.OutputStream
import java.lang.Integer.min
import java.net.URLEncoder
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.FileTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** package name */
const val PACKAGE = "tech.simter.file"

/**
 * The default admin role code to manage this module.
 *
 * Can be override through yam key [ADMIN_ROLE_KEY] value.
 */
const val DEFAULT_ADMIN_ROLE = "ADMIN"

/** The default module value */
const val DEFAULT_MODULE_VALUE = "/default/"

/** Admin role code config key */
const val ADMIN_ROLE_KEY = "simter-file.authorization.admin-role"

/** Default module authorizer key */
const val DEFAULT_MODULE_AUTHORIZER_KEY = "simter-file.authorization.default"

/** All business modules authorizer key */
const val MODULES_AUTHORIZER_KEY = "simter-file..authorization.modules"

/** The config key for config the root directory to store files */
const val BASE_DATA_DIR = "simter-file.base-data-dir"

/** table name of file */
const val TABLE_FILE = "st_file"

/** the read operation key */
const val OPERATION_READ = "READ"

/** the create operation key */
const val OPERATION_CREATE = "CREATE"

/** the update operation key */
const val OPERATION_UPDATE = "UPDATE"

/** the delete operation key */
const val OPERATION_DELETE = "DELETE"

/** the standard module separator */
const val MODULE_SEPARATOR = "/"

/** A timestamp id datetime formatter with pattern 'yyyyMMddTHHmmssSSSS' */
val TIMESTAMP_ID_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSSS")

/** A simple kotlin json*/
val kotlinJson = JsonUtils.json

/**
 * Generate a string with timestamp as prefix and limit-len uuid as suffix.
 *
 * The format is '${yyyyMMddTHHmmssSSSS}-${randomUUIDPrefix}'
 */
fun timestampId(
  ts: Optional<OffsetDateTime> = Optional.empty(),
  uuid: Optional<UUID> = Optional.empty(),
  uuidLen: Int = 6
): String {
  return if (uuidLen > 0) {
    ts.orElseGet { OffsetDateTime.now() }.format(TIMESTAMP_ID_FORMATTER) +
      "-" +
      uuid.orElseGet { UUID.randomUUID() }.toString().substring(0, min(uuidLen, 36))
  } else ts.orElseGet { OffsetDateTime.now() }.format(TIMESTAMP_ID_FORMATTER)
}

/**
 * Format the module value to the standard format
 * 1. Add '/' prefix if it's not start with it.
 * 2. Add '/' suffix if it's not end with it.
 */
fun standardModuleValue(module: String): String {
  return if (module.startsWith(MODULE_SEPARATOR)) {
    if (module.endsWith(MODULE_SEPARATOR) || module.endsWith("$MODULE_SEPARATOR%")) module
    else module + MODULE_SEPARATOR
  } else {
    if (module.endsWith(MODULE_SEPARATOR) || module.endsWith("$MODULE_SEPARATOR%")) MODULE_SEPARATOR + module
    else MODULE_SEPARATOR + module + MODULE_SEPARATOR
  }
}

/**
 * Build a http 'Content-Disposition' header value.
 *
 * Return a value with pattern `$type; filename="...ISO_8859_1..." filename*="...UTF-8..."`.
 */
fun buildContentDisposition(type: String, filename: String): String {
  val filename8859 = String(filename.toByteArray(), Charsets.ISO_8859_1)
  val filenameUtf8 = URLEncoder.encode(filename, "UTF-8")
  return type +
    "; filename=\"$filename8859\"" +
    "; filename*=\"UTF-8''$filenameUtf8\""
}

/**
 * Compress all [files] to the [outputStream].
 *
 * Pack all files with zip file format.
 * And the zip file has a same structure with the module path value.
 * Caller can use the [moduleMapper] to map the module path value to another path name.
 *
 * Example 1:
 * ```
 * /A/a1/filename1
 * /A/a1/filename2
 * will zip to x.zip with structure:
 * A/
 * |--a1/
 * |  |--filename1
 * |  |--filename2
 * ```
 *
 * Example 2:
 * ```
 * /A/a1/filename1
 * /A/a2/filename1
 * will zip to x.zip with structure:
 * A/
 * |--a1/
 * |  |--filename1
 * |--a2/
 * |  |--filename1
 * ```
 *
 * Example 3 with moduleMapper={"/A/a1/": "Aa1", "/A/a2/": "Aa2"}:
 * ```
 * /A/a1/filename1
 * /A/a2/filename1
 * will zip to x.zip with structure:
 * Aa1/
 * |--filename1
 * Aa2/
 * |--filename1
 * ```
 *
 * @param[moduleMapper] map [FilePack.module] value to another value,
 *                      this effect the zip entry folder name.
 *                      Key '_' for map all the missing key.
 * @param[autoClose] whether auto close the target output stream when finished pack,
 *                   default true
 * @param[basePath] the base path to resolve the [FilePack.path] file
 */
fun packFilesTo(
  outputStream: OutputStream,
  files: List<FilePack>,
  basePath: Path,
  moduleMapper: Map<String, String>,
  autoClose: Boolean = true
) {
  // init zip file
  val zipOutputStream = ZipOutputStream(outputStream)
  val zipChannel = Channels.newChannel(zipOutputStream)

  // zip all files
  files.map { file ->
    // 1. map origin `$module` to the custom name
    val mappedName = Paths.get(moduleMapper.getOrDefault(
      file.module,
      moduleMapper.getOrDefault("_", file.module) // "_" for fallback to map everything
    ), file.fileName)

    // 2. remove start slash to avoid create the zip root folder '_'
    val zipEntryName = if (mappedName.isAbsolute) mappedName.toString().substring(1)
    else mappedName.toString()

    // 3. add zip entry
    val zipEntry = ZipEntry(zipEntryName)
    zipEntry.creationTime = FileTime.from(file.createOn.toInstant())
    zipEntry.lastModifiedTime = FileTime.from(file.modifyOn.toInstant())
    zipOutputStream.putNextEntry(zipEntry)

    // 4. write file data to zip file
    // zero-copy to zip stream
    val fileChannel = FileChannel.open(basePath.resolve(file.path), StandardOpenOption.READ)
    fileChannel.transferTo(0, Long.MAX_VALUE, zipChannel)
    fileChannel.close()

    zipOutputStream.closeEntry()
  }
  zipChannel.close()

  // close output stream
  if (autoClose) zipOutputStream.close()
}