package tech.simter.file.rest.webflux

import java.util.*

/**
 * Webflux's unit test tool class.
 *
 * @author zh
 */
object TestHelper {
  /** random [Int] in open interval from [start] and [end]]*/
  fun randomInt(start: Int = 0, end: Int = 100) = Random().nextInt(end + 1 - start) + start

  private var strMap = hashMapOf<String, Int>()
  /** random unique [String] to the [prefix]  */
  fun randomString(prefix: String = "Str"): String {
    if (!strMap.containsKey(prefix)) strMap[prefix] = 1
    else strMap[prefix] = strMap[prefix]!! + 1
    return "$prefix${strMap[prefix]}"
  }
}