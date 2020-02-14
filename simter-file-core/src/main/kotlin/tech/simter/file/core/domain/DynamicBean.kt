package tech.simter.file.core.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.MappedSuperclass
import kotlin.reflect.KProperty

/**
 * 动态 Bean 基类。
 *
 * 特别注意子类中的属性需要定义为非只读属性 `var` 而不是 `val`，并且全部属性需要定义为可空，且不要设置任何默认值。
 *
 * @author RJ
 */
@MappedSuperclass
open class DynamicBean {
  /** 属性数据持有者 */
  @get:JsonIgnore
  @get:javax.persistence.Transient
  @get:org.springframework.data.annotation.Transient
  protected val holder: DataHolder = DataHolder()
  @get:JsonIgnore
  @get:javax.persistence.Transient
  @get:org.springframework.data.annotation.Transient
  val data: Map<String, Any?>
    get() = holder.map

  override fun toString(): String {
    return "${javaClass.simpleName}=$holder"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DynamicBean

    if (holder != other.holder) return false

    return true
  }

  override fun hashCode(): Int {
    return holder.hashCode()
  }

  /**
   * 动态 Bean 属性值的数据持有者。
   *
   * 这个实现有点类似于 [Map]，通过 [map] 属性来获取动态 Bean 被设置了的属性键值对，键为 Bean 的属性名称，
   * 值为 Bean 相应的属性值。如果 Bean 的属性并没有设置过，则不会在 [map] 中有相应的键值对，只有被设置过
   * 的属性才会保存相应的键值对在 [map] 中。[size] 属性则返回已设置过的属性的数量。
   *
   * 相关技术请参考 [Kotlin Delegated Properties](https://kotlinlang.org/docs/reference/delegated-properties.html)。
   *
   * @author RJ
   */
  data class DataHolder(
    private val data: MutableMap<String, Any?> = mutableMapOf<String, Any?>().withDefault { null }
  ) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> getValue(thisRef: Any?, property: KProperty<*>): T {
      return data[property.name] as T
    }

    operator fun <T> setValue(thisRef: Any?, property: KProperty<*>, value: T) {
      data[property.name] = value
    }

    val size get() = data.size
    val map: Map<String, Any?> get() = data
    fun isEmpty(): Boolean = data.isEmpty()

    override fun toString(): String {
      return data.toString()
    }
  }
}
