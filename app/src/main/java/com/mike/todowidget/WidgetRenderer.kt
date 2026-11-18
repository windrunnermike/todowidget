package com.mike.todowidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews

object WidgetRenderer {

    private val TODO_ROWS = intArrayOf(
        R.id.t_row0, R.id.t_row1, R.id.t_row2, R.id.t_row3,
        R.id.t_row4, R.id.t_row5, R.id.t_row6, R.id.t_row7
    )
    private val TODO_TITLE = intArrayOf(
        R.id.t_title0, R.id.t_title1, R.id.t_title2, R.id.t_title3,
        R.id.t_title4, R.id.t_title5, R.id.t_title6, R.id.t_title7
    )
    private val TODO_SUB = intArrayOf(
        R.id.t_sub0, R.id.t_sub1, R.id.t_sub2, R.id.t_sub3,
        R.id.t_sub4, R.id.t_sub5, R.id.t_sub6, R.id.t_sub7
    )
    private val TODO_CHIP = intArrayOf(
        R.id.t_chip0, R.id.t_chip1, R.id.t_chip2, R.id.t_chip3,
        R.id.t_chip4, R.id.t_chip5, R.id.t_chip6, R.id.t_chip7
    )
    private val TODO_CHECK = intArrayOf(
        R.id.t_check0, R.id.t_check1, R.id.t_check2, R.id.t_check3,
        R.id.t_check4, R.id.t_check5, R.id.t_check6, R.id.t_check7
    )

    private val LONG_ROWS = intArrayOf(
        R.id.l_row0, R.id.l_row1, R.id.l_row2, R.id.l_row3,
        R.id.l_row4, R.id.l_row5, R.id.l_row6, R.id.l_row7
    )
    private val LONG_TITLE = intArrayOf(
        R.id.l_title0, R.id.l_title1, R.id.l_title2, R.id.l_title3,
        R.id.l_title4, R.id.l_title5, R.id.l_title6, R.id.l_title7
    )
    private val LONG_SUB = intArrayOf(
        R.id.l_sub0, R.id.l_sub1, R.id.l_sub2, R.id.l_sub3,
        R.id.l_sub4, R.id.l_sub5, R.id.l_sub6, R.id.l_sub7
    )
    private val LONG_CHIP = intArrayOf(
        R.id.l_chip0, R.id.l_chip1, R.id.l_chip2, R.id.l_chip3,
        R.id.l_chip4, R.id.l_chip5, R.id.l_chip6, R.id.l_chip7
    )
    private val LONG_BAR = intArrayOf(
        R.id.l_bar0, R.id.l_bar1, R.id.l_bar2, R.id.l_bar3,
        R.id.l_bar4, R.id.l_bar5, R.id.l_bar6, R.id.l_bar7
    )

    fun refreshAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        mgr.getAppWidgetIds(ComponentName(ctx, TodoWidgetProvider::class.java))
            .forEach { renderTodo(ctx, mgr, it) }
        mgr.getAppWidgetIds(ComponentName(ctx, DdlWidgetProvider::class.java))
            .forEach { renderDdl(ctx, mgr, it) }
        Reminder.schedule(ctx)
    }

    // ---------------------------------------------------------------- 大组件
    fun renderTodo(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {
        val rv = RemoteViews(ctx.packageName, R.layout.widget_todo)
        val all = Store.load(ctx)
        val now = System.currentTimeMillis()

        rv.setTextViewText(R.id.date_text, Fmt.headerDate(now))
        rv.setTextViewText(R.id.weekday_text, Fmt.weekday(now))
        rv.setTextViewText(R.id.done_count, Fmt.doneCount(all, now))
        rv.setTextViewText(R.id.summary_text, Fmt.summary(all, now))
        rv.setProgressBar(R.id.today_progress, 100, Fmt.todayPercent(all, now), false)

        // 最近 DDL 提示条
        val nearest = Fmt.nearestDdl(all, now)
        if (nearest == null) {
            rv.setViewVisibility(R.id.ddl_band, View.GONE)
        } else {
            rv.setViewVisibility(R.id.ddl_band, View.VISIBLE)
            rv.setTextViewText(
                R.id.ddl_band_text,
                "最近 DDL  ${Fmt.shortDate(nearest.deadline)} ${Fmt.time(nearest.deadline)} · ${nearest.title}"
            )
            val c = Fmt.chipOf(ctx, nearest, now)
            rv.setTextViewText(R.id.ddl_band_chip, c.text)
            rv.setTextColor(R.id.ddl_band_chip, c.fgColor)
            rv.setInt(R.id.ddl_band_chip, "setBackgroundResource", c.bgRes)
        }

        val capacity = capacityOf(mgr, widgetId)
        val todayCap = (capacity * 0.55f).toInt().coerceAtLeast(1)
        val longCap = (capacity - todayCap).coerceAtLeast(1)

        val today = Fmt.todayRows(all, now, todayCap)
        val longs = Fmt.longRows(all, now, longCap)

        val grey = ctx.getColor(R.color.text_tertiary)
        val primary = ctx.getColor(R.color.text_primary)

        // 今日待办
        for (i in 0 until 8) {
            if (i < today.size) {
                val t = today[i]
                rv.setViewVisibility(TODO_ROWS[i], View.VISIBLE)
                rv.setImageViewResource(TODO_CHECK[i], if (t.done) R.drawable.ic_check_done else R.drawable.ic_check_todo)
                rv.setTextViewText(TODO_TITLE[i], t.title)
                rv.setTextViewText(TODO_SUB[i], Fmt.subOf(t, now))
                if (t.done) {
                    rv.setInt(TODO_TITLE[i], "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
                    rv.setTextColor(TODO_TITLE[i], grey)
                    rv.setFloat(TODO_SUB[i], "setAlpha", 0.6f)
                } else {
                    rv.setInt(TODO_TITLE[i], "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
                    rv.setTextColor(TODO_TITLE[i], primary)
                    rv.setFloat(TODO_SUB[i], "setAlpha", 1f)
                }
                val c = Fmt.chipOf(ctx, t, now)
                rv.setTextViewText(TODO_CHIP[i], if (t.done) "" else c.text)
                rv.setTextColor(TODO_CHIP[i], c.fgColor)
                rv.setInt(TODO_CHIP[i], "setBackgroundResource", if (t.done) 0 else c.bgRes)
                rv.setViewVisibility(TODO_CHIP[i], if (t.done) View.GONE else View.VISIBLE)
                rv.setOnClickPendingIntent(TODO_ROWS[i], toggleIntent(ctx, t.id, i))
            } else {
                rv.setViewVisibility(TODO_ROWS[i], View.GONE)
            }
        }

        // 长期待办
        for (i in 0 until 8) {
            if (i < longs.size) {
                val t = longs[i]
                rv.setViewVisibility(LONG_ROWS[i], View.VISIBLE)
                rv.setTextViewText(LONG_TITLE[i], t.title)
                rv.setTextViewText(LONG_SUB[i], Fmt.subOf(t, now))
                val c = Fmt.chipOf(ctx, t, now)
                rv.setInt(LONG_BAR[i], "setBackgroundResource", c.barRes)
                if (t.done) {
                    rv.setInt(LONG_TITLE[i], "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
                    rv.setTextColor(LONG_TITLE[i], grey)
                    rv.setFloat(LONG_SUB[i], "setAlpha", 0.6f)
                    rv.setViewVisibility(LONG_CHIP[i], View.GONE)
                } else {
                    rv.setInt(LONG_TITLE[i], "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
                    rv.setTextColor(LONG_TITLE[i], primary)
                    rv.setFloat(LONG_SUB[i], "setAlpha", 1f)
                    rv.setViewVisibility(LONG_CHIP[i], View.VISIBLE)
                    rv.setTextViewText(LONG_CHIP[i], c.text)
                    rv.setTextColor(LONG_CHIP[i], c.fgColor)
                    rv.setInt(LONG_CHIP[i], "setBackgroundResource", c.bgRes)
                }
                rv.setOnClickPendingIntent(LONG_ROWS[i], toggleIntent(ctx, t.id, 100 + i))
            } else {
                rv.setViewVisibility(LONG_ROWS[i], View.GONE)
            }
        }

        // 空状态
        val empty = all.isEmpty()
        rv.setViewVisibility(R.id.empty_hint, if (empty) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.today_header, if (empty) View.GONE else View.VISIBLE)
        rv.setViewVisibility(R.id.divider, if (empty) View.GONE else View.VISIBLE)
        rv.setViewVisibility(R.id.long_header, if (empty) View.GONE else View.VISIBLE)

        rv.setOnClickPendingIntent(R.id.add_btn, openApp(ctx, 200, "new"))
        rv.setOnClickPendingIntent(R.id.settings_btn, openApp(ctx, 201, "all"))
        rv.setOnClickPendingIntent(R.id.today_all, openApp(ctx, 202, "all"))
        rv.setOnClickPendingIntent(R.id.long_manage, openApp(ctx, 203, "all"))

        mgr.updateAppWidget(widgetId, rv)
    }

    // ------------------------------------------------------------ 2x2 DDL 组件
    fun renderDdl(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {
        val rv = RemoteViews(ctx.packageName, R.layout.widget_ddl)
        val all = Store.load(ctx)
        val now = System.currentTimeMillis()
        val next = Fmt.sortForWidget(all, now).firstOrNull { it.deadline > 0L }

        if (next == null) {
            rv.setTextViewText(R.id.ddl_number, "--")
            rv.setTextViewText(R.id.ddl_unit, "")
            rv.setTextViewText(R.id.ddl_title, "暂无待办")
            rv.setTextViewText(R.id.ddl_date, "")
        } else {
            val delta = next.deadline - now
            val num: String
            val unit: String
            if (delta < 0) {
                val over = -delta
                num = (over / Fmt.DAY).coerceAtLeast(1).toString()
                unit = "天前逾期"
            } else if (delta >= Fmt.DAY) {
                num = (delta / Fmt.DAY).toString()
                unit = "天后"
            } else if (delta >= Fmt.HOUR) {
                num = (delta / Fmt.HOUR).toString()
                unit = "小时后"
            } else {
                num = (delta / 60000L).coerceAtLeast(1).toString()
                unit = "分钟后"
            }
            rv.setTextViewText(R.id.ddl_number, num)
            rv.setTextViewText(R.id.ddl_unit, unit)
            rv.setTextViewText(R.id.ddl_title, next.title)
            rv.setTextViewText(
                R.id.ddl_date,
                "${Fmt.shortDate(next.deadline)} ${Fmt.time(next.deadline)} 截止"
            )
            val c = Fmt.chipOf(ctx, next, now)
            rv.setTextColor(R.id.ddl_number, c.fgColor)
        }
        rv.setOnClickPendingIntent(R.id.ddl_number, openApp(ctx, 210, "all"))
        rv.setOnClickPendingIntent(R.id.ddl_title, openApp(ctx, 211, "all"))
        mgr.updateAppWidget(widgetId, rv)
    }

    // ---------------------------------------------------------------- helpers
    private fun capacityOf(mgr: AppWidgetManager, widgetId: Int): Int {
        val opts: Bundle = mgr.getAppWidgetOptions(widgetId)
        val hDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 380)
        return ((hDp - 150) / 40).coerceIn(2, 8)
    }

    private fun toggleIntent(ctx: Context, id: String, req: Int): PendingIntent {
        val i = Intent(ctx, ToggleReceiver::class.java)
            .setAction(ToggleReceiver.ACTION_TOGGLE)
            .putExtra(ToggleReceiver.EXTRA_ID, id)
            .setData(Uri.parse("todowidget://toggle/$id"))
        return PendingIntent.getBroadcast(
            ctx, req, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openApp(ctx: Context, req: Int, mode: String): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(MainActivity.EXTRA_MODE, mode)
            .setData(Uri.parse("todowidget://open/$mode"))
        return PendingIntent.getActivity(
            ctx, req, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
