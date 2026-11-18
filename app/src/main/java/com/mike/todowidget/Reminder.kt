package com.mike.todowidget

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Calendar

object Reminder {

    const val CHANNEL_ID = "todo_reminder"
    const val ACTION_ALARM = "com.mike.todowidget.action.ALARM"
    private const val RC_ALARM = 4242

    private fun alarmIntent(ctx: Context): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java)
            .setAction(ACTION_ALARM)
            .setData(Uri.parse("todowidget://alarm"))
        return PendingIntent.getBroadcast(
            ctx, RC_ALARM, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(CHANNEL_ID, "待办提醒", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "DDL 前一小时提醒、每日 9 点汇总"
            nm.createNotificationChannel(ch)
        }
    }

    /** 只排一个闹钟：下一个 DDL 前一小时，和下一个 9:00，取更早的那个 */
    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = alarmIntent(ctx)
        am.cancel(pi)

        val now = System.currentTimeMillis()
        val nextDeadline = Store.load(ctx)
            .filter { !it.done && it.deadline > now }
            .minOfOrNull { it.deadline }

        var trigger = nextNineAm(now)
        if (nextDeadline != null) {
            val remindAt = nextDeadline - Fmt.HOUR
            if (remindAt > now && remindAt < trigger) trigger = remindAt
        }
        if (trigger <= now) trigger = now + 60_000

        val canExact = am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        } catch (e: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    private fun nextNineAm(now: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = now
        c.set(Calendar.HOUR_OF_DAY, 9)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        if (c.timeInMillis <= now) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    /** 闹钟响了：该提醒的提醒，然后重排下一次 */
    fun fire(ctx: Context) {
        ensureChannel(ctx)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.areNotificationsEnabled()) return

        val now = System.currentTimeMillis()
        val tasks = Store.load(ctx)

        // 1) 一小时内到期的任务
        tasks.filter { !it.done && it.deadline > now && it.deadline - now <= 70 * 60_000L }
            .forEach { t ->
                notify(
                    ctx,
                    t.id.hashCode(),
                    "还剩 ${((t.deadline - now) / 60000L).coerceAtLeast(1)} 分钟",
                    t.title
                )
            }

        // 2) 早上 9 点汇总
        val c = Calendar.getInstance()
        c.timeInMillis = now
        if (c.get(Calendar.HOUR_OF_DAY) in 8..10) {
            val open = tasks.count { !it.done }
            val over = tasks.count { !it.done && it.deadline in 1L..now }
            val nearest = Fmt.nearestDdl(tasks, now)
            val text = buildString {
                append("今天还有 ").append(open).append(" 项")
                if (over > 0) append("，逾期 ").append(over).append(" 项")
                if (nearest != null) {
                    append("；最近：").append(nearest.title)
                    append("（").append(((nearest.deadline - now) / Fmt.DAY)).append(" 天后）")
                }
            }
            notify(ctx, 9001, "早上好，今天的待办", text)
        }
    }

    private fun notify(ctx: Context, id: Int, title: String, text: String) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pi = PendingIntent.getActivity(
            ctx, id,
            Intent(ctx, MainActivity::class.java).setData(Uri.parse("todowidget://notify/$id")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n: Notification = Notification.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setColor(ctx.getColor(R.color.accent))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(id, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Reminder.ACTION_ALARM) return
        Reminder.fire(context)
        Reminder.schedule(context)
    }
}

