package com.mike.todowidget

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 一块徽章的视觉数据 */
data class Chip(val text: String, val bgRes: Int, val fgColor: Int, val barRes: Int)

object Fmt {

    const val HOUR = 3600_000L
    const val DAY = 86_400_000L

    private val WEEKDAY = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

    fun headerDate(ms: Long): String =
        SimpleDateFormat("M月d日", Locale.CHINA).format(Date(ms))

    fun weekday(ms: Long): String {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        return WEEKDAY[c.get(Calendar.DAY_OF_WEEK) - 1]
    }

    fun shortDate(ms: Long): String =
        SimpleDateFormat("MM/dd", Locale.CHINA).format(Date(ms))

    fun fullDate(ms: Long): String =
        SimpleDateFormat("yyyy/MM/dd", Locale.CHINA).format(Date(ms))

    fun time(ms: Long): String =
        SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(ms))

    fun dayStart(ms: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun isSameDay(a: Long, b: Long): Boolean = dayStart(a) == dayStart(b)

    /** 徽章文案 + 颜色分级：逾期 < 24h < 3天 < 14天 < 其余 */
    fun chipOf(ctx: Context, t: Task, now: Long): Chip {
        val redFillBg = R.drawable.chip_redfill
        val redBg = R.drawable.chip_red
        val orangeBg = R.drawable.chip_orange
        val amberBg = R.drawable.chip_amber
        val greyBg = R.drawable.chip_grey

        val white = 0xFFFFFFFF.toInt()
        val redFg = ctx.getColor(R.color.chip_red_text)
        val orangeFg = ctx.getColor(R.color.chip_orange_text)
        val amberFg = ctx.getColor(R.color.chip_amber_text)
        val greyFg = ctx.getColor(R.color.chip_grey_text)

        if (t.deadline <= 0L) {
            val text = fallbackText(t)
            return Chip(text, greyBg, greyFg, R.drawable.bar_grey)
        }

        val delta = t.deadline - now
        return when {
            delta < 0L -> {
                val over = -delta
                val text = if (over < DAY) {
                    "逾期 ${(over / HOUR).coerceAtLeast(1)} 小时"
                } else {
                    "逾期 ${over / DAY} 天"
                }
                Chip(text, redFillBg, white, R.drawable.bar_red)
            }

            delta < HOUR -> Chip("剩 ${(delta / 60000L).coerceAtLeast(1)} 分钟", redBg, redFg, R.drawable.bar_red)
            delta < DAY -> Chip("剩 ${delta / HOUR} 小时", redBg, redFg, R.drawable.bar_red)
            delta <= 3 * DAY -> Chip("剩 ${delta / DAY} 天", orangeBg, orangeFg, R.drawable.bar_orange)
            delta <= 14 * DAY -> Chip("剩 ${delta / DAY} 天", amberBg, amberFg, R.drawable.bar_amber)
            delta <= 60 * DAY -> Chip("剩 ${delta / DAY} 天", greyBg, greyFg, R.drawable.bar_blue)
            else -> Chip("剩 ${delta / (30 * DAY)} 个月", greyBg, greyFg, R.drawable.bar_grey)
        }
    }

    /** 没有截止时间时，徽章退回到「计数 / 百分比」 */
    private fun fallbackText(t: Task): String {
        if (t.counter.isNotBlank()) return t.counter
        if (t.progress > 0) return "${t.progress}%"
        return "无期限"
    }

    /** 标题下面那行小字 */
    fun subOf(t: Task, now: Long): String {
        if (t.done) {
            return if (t.doneAt > 0L) "已完成 ${time(t.doneAt)}" else "已完成"
        }
        if (t.deadline <= 0L) {
            return if (t.counter.isNotBlank()) "进度 ${t.counter}" else "无截止日"
        }
        val day = if (isSameDay(t.deadline, now)) {
            "今天"
        } else if (isSameDay(t.deadline, now + DAY)) {
            "明天"
        } else if (t.deadline < now) {
            shortDate(t.deadline) + " 已逾期"
        } else {
            shortDate(t.deadline)
        }
        return "截止 $day ${time(t.deadline)}"
    }

    /** 排序权重：逾期 0 → 24h 1 → 3 天 2 → 有期限 3 → 无期限 4 */
    fun rank(t: Task, now: Long): Int {
        if (t.deadline <= 0L) return 4
        val d = t.deadline - now
        return when {
            d < 0L -> 0
            d < DAY -> 1
            d <= 3 * DAY -> 2
            else -> 3
        }
    }

    fun sortForWidget(list: List<Task>, now: Long): List<Task> =
        list.filter { !it.done }
            .sortedWith(compareBy({ rank(it, now) }, { if (it.deadline <= 0L) Long.MAX_VALUE else it.deadline }))

    /** 今日待办：未完成优先，已完成的沉到下面 */
    fun todayRows(all: List<Task>, now: Long, cap: Int): List<Task> {
        val today = all.filter { !it.isLong() }
        val done = today.filter { it.done }.sortedByDescending { it.doneAt }
        val open = sortForWidget(today, now)
        return (open + done).take(cap)
    }

    /** 长期待办：未完成优先，然后按紧急度 */
    fun longRows(all: List<Task>, now: Long, cap: Int): List<Task> {
        val longs = all.filter { it.isLong() }
        val done = longs.filter { it.done }.sortedByDescending { it.doneAt }
        val open = sortForWidget(longs, now)
        return (open + done).take(cap)
    }

    /** 最近一条未完成且有 DDL 的任务 */
    fun nearestDdl(all: List<Task>, now: Long): Task? =
        all.filter { !it.done && it.deadline > 0L }
            .minByOrNull { it.deadline }
            ?.takeIf { it.deadline > now }

    /**
     * 汇总行：今天 5 项 · 长期 5 项 · 逾期 1 项
     */
    fun summary(all: List<Task>, now: Long): String {
        val todayOpen = all.count { !it.isLong() && !it.done }
        val longOpen = all.count { it.isLong() && !it.done }
        val overdue = all.count { !it.done && it.deadline > 0L && it.deadline < now }
        val sb = StringBuilder()
        sb.append("今天 ").append(todayOpen).append(" 项 · 长期 ").append(longOpen).append(" 项")
        if (overdue > 0) sb.append(" · 逾期 ").append(overdue).append(" 项")
        return sb.toString()
    }

    /** 今日完成 x/y */
    fun doneCount(all: List<Task>, now: Long): String {
        val today = all.filter { !it.isLong() }
        val done = today.count { it.done && (it.doneAt == 0L || isSameDay(it.doneAt, now)) }
        val total = today.size
        if (total == 0) return "今日还没有任务"
        return "今日完成 $done/$total"
    }

    /** 今日进度百分比 */
    fun todayPercent(all: List<Task>, now: Long): Int {
        val today = all.filter { !it.isLong() }
        if (today.isEmpty()) return 0
        val done = today.count { it.done && (it.doneAt == 0L || isSameDay(it.doneAt, now)) }
        return (done * 100 / today.size).coerceIn(0, 100)
    }
}
