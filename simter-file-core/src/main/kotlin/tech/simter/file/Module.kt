package tech.simter.file

/** package name */
const val PACKAGE = "tech.simter.file"

/**
 * The default admin role code to manage this module.
 *
 * Can be override through yam key [ADMIN_ROLE_KEY] value.
 */
const val DEFAULT_ADMIN_ROLE = "ADMIN"
/** Admin role code config key */
const val ADMIN_ROLE_KEY = "module.authorization.simter-file.admin-role"
/** Default module authorizer key */
const val DEFAULT_MODULE_AUTHORIZER_KEY = "module.authorization.simter-file.default"
/** All business modules authorizer key */
const val MODULES_AUTHORIZER_KEY = "module.authorization.simter-file.modules"

/** table name of attachment */
const val TABLE_ATTACHMENT = "st_attachment"

/** the read operation key */
const val OPERATION_READ = "READ"
/** the create operation key */
const val OPERATION_CREATE = "CREATE"
/** the update operation key */
const val OPERATION_UPDATE = "UPDATE"
/** the delete operation key */
const val OPERATION_DELETE = "DELETE"