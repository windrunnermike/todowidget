package com.mike.todowidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

class TodoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { WidgetRenderer.renderTodo(context, appWidgetManager, it) }
        Reminder.schedule(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        WidgetRenderer.renderTodo(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        Reminder.schedule(context)
    }
}

class DdlWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { WidgetRenderer.renderDdl(context, appWidgetManager, it) }
        Reminder.schedule(context)
    }

    override fun onEnabled(context: Context) {
        Reminder.schedule(context)
    }
}
