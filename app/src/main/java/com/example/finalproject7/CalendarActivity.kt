
package com.example.finalproject7

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CalendarActivity : ComponentActivity() {

    private lateinit var returnButton2: Button
    private lateinit var calendarGrid: GridLayout
    private var taskList = mutableListOf<Task>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // Initialize views
        returnButton2 = findViewById(R.id.returnButton2)
        calendarGrid = findViewById(R.id.calendarGrid)

        // Load tasks from SharedPreferences
        loadTasks()

        // Set up the calendar for the current month (April 2025)
        setupCalendar(YearMonth.of(2025, 4)) // Static month: April 2025

        // Return button listener
        returnButton2.setOnClickListener {
            finish() // Close the activity and return
        }
    }

    private fun setupCalendar(yearMonth: YearMonth) {
        // Get the first day of the month and the number of days in the month
        val firstDay = yearMonth.atDay(1)
        val daysInMonth = yearMonth.lengthOfMonth()
        val firstDayOfWeek = firstDay.dayOfWeek.value % 7 // Sunday = 0, Monday = 1, etc.

        // Set up the grid: 7 columns (days of the week), enough rows for the month
        calendarGrid.columnCount = 7
        calendarGrid.rowCount = 6 // Max 6 weeks to cover any month

        // Day of week headers
        val daysOfWeek = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        for (i in 0..6) {
            val dayHeader = TextView(this).apply {
                text = daysOfWeek[i]
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setTextColor(ContextCompat.getColor(this@CalendarActivity, R.color.black))
                setPadding(8, 8, 8, 8)
            }
            calendarGrid.addView(dayHeader, GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(0)
                columnSpec = GridLayout.spec(i)
            })
        }

        // Fill in the days of the month
        var day = 1
        for (row in 1..5) { // Rows 1-5 for days (row 0 is headers)
            for (col in 0..6) {
                val cell = TextView(this).apply {
                    textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    setPadding(8, 8, 8, 8)
                }

                if (row == 1 && col < firstDayOfWeek) {
                    // Empty cells before the first day
                    cell.text = ""
                } else if (day <= daysInMonth) {
                    // Add the day number
                    cell.text = day.toString()

                    // Check if this day has tasks
                    val currentDate = yearMonth.atDay(day)
                    val tasksForDay = taskList.filter { it.dueDate == currentDate }
                    if (tasksForDay.isNotEmpty()) {
                        // Highlight days with tasks (e.g., red for assignments, blue for tasks)
                        val hasAssignment = tasksForDay.any { it.type == "Assignment" }
                        val hasTask = tasksForDay.any { it.type == "Task" }
                        cell.setBackgroundColor(
                            when {
                                hasAssignment -> Color.LTGRAY // Light gray for assignments
                                hasTask -> Color.LTGRAY       // Light gray for tasks (could differentiate if needed)
                                else -> Color.TRANSPARENT
                            }
                        )
                        cell.setTextColor(
                            when {
                                hasAssignment -> Color.RED   // Red text for assignments
                                hasTask -> Color.BLUE        // Blue text for tasks
                                else -> Color.BLACK
                            }
                        )
                    }

                    day++
                } else {
                    // Empty cells after the last day
                    cell.text = ""
                }

                calendarGrid.addView(cell, GridLayout.LayoutParams().apply {
                    rowSpec = GridLayout.spec(row)
                    columnSpec = GridLayout.spec(col)
                })
            }
        }
    }

    private fun loadTasks() {
        val sharedPreferences = getSharedPreferences("tasks", Context.MODE_PRIVATE)
        val taskStrings = sharedPreferences.getStringSet("taskList", emptySet()) ?: emptySet()
        taskList.addAll(taskStrings.map { Task.fromFormattedString(it) })
    }
}



