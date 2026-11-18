package com.mike.todowidget

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 一条待办。deadline = 0 表示没有截止时间。
 * type: "today" 每日待办 / "long" 长期待办
 */
data class Task(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var type: String = TYPE_TODAY,
    var deadline: Long = 0L,
    var done: Boolean = false,
    var doneAt: Long = 0L,
    var counter: String = "",
    var progress: Int = 0,
    var createdAt: Long = System.currentTimeMillis()
) {

    fun isLong(): Boolean = type == TYPE_LONG

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("type", type)
        put("deadline", deadline)
        put("done", done)
        put("doneAt", doneAt)
        put("counter", counter)
        put("progress", progress)
        put("createdAt", createdAt)
    }

    companion object {
        const val TYPE_TODAY = "today"
        const val TYPE_LONG = "long"

        fun fromJson(o: JSONObject): Task = Task(
            id = o.optString("id", UUID.randomUUID().toString()),
            title = o.optString("title", ""),
            type = o.optString("type", TYPE_TODAY),
            deadline = o.optLong("deadline", 0L),
            done = o.optBoolean("done", false),
            doneAt = o.optLong("doneAt", 0L),
            counter = o.optString("counter", ""),
            progress = o.optInt("progress", 0),
            createdAt = o.optLong("createdAt", 0L)
        )

        fun listToJson(list: List<Task>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }

        fun listFromJson(raw: String?): MutableList<Task> {
            val out = mutableListOf<Task>()
            if (raw.isNullOrBlank()) return out
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    out.add(fromJson(o))
                }
            } catch (e: Exception) {
                // 数据损坏时宁可从空开始，也不要让组件崩掉
            }
            return out
        }
    }
}
