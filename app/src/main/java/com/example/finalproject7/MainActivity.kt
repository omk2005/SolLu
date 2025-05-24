package com.example.finalproject7

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject7.databinding.ActivityMainBinding
import com.example.finalproject7.databinding.TaskItemBinding
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import android.widget.Toast
import android.app.NotificationManager // Added import

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private var taskList = mutableListOf<Task>()
    private lateinit var inputActivityLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create notification channel
        NotificationReceiver.createNotificationChannel(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tasksRecyclerView = binding.tasksRecyclerView

        // Clear all notifications and alarms
        clearAllNotificationsAndAlarms()

        // Set up RecyclerView
        tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter(taskList, { task ->
            removeTask(task)
        })
        tasksRecyclerView.adapter = taskAdapter

        // Load tasks from SharedPreferences
        loadTasks()

        // Add a sample task for testing
        if (taskList.isEmpty()) {
            val sampleTask = Task(
                "Assignment",
                "Complete Math Homework",
                "Finish all problems in chapter 3",
                LocalDate.now().plusDays(1), // Due tomorrow
                LocalTime.of(17, 0), // 5:00 PM
                isNotificationEnabled = false // Disable notifications
            )
            taskList.add(sampleTask)
            taskAdapter.notifyItemInserted(taskList.size - 1)
        }

        // Set up permission launcher for POST_NOTIFICATIONS
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted, schedule notifications for existing tasks
                taskList.forEach { scheduleNotification(it) }
            } else {
                // Permission denied, inform the user
                Toast.makeText(this, "Notification permission denied. Reminders will not work.", Toast.LENGTH_SHORT).show()
            }
        }

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Set up button click listeners
        binding.inputPageButton.setOnClickListener {
            val intent = Intent(this, InputActivity::class.java)
            inputActivityLauncher.launch(intent)
        }

        binding.calendarButton.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }

        binding.mapButton.setOnClickListener {
            val intent = Intent(this, CampusActivity::class.java)
            startActivity(intent)
        }

        inputActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val newTask = result.data?.getParcelableExtra<Task>("newTask")
                if (newTask != null) {
                    // Assign a unique notification ID
                    newTask.notificationId = taskList.size + 1
                    taskList.add(newTask)
                    taskAdapter.notifyItemInserted(taskList.size - 1)
                    saveTasks()
                    scheduleNotification(newTask)
                }
            }
        }
    }

    private fun clearAllNotificationsAndAlarms() {
        // Clear all notifications
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationManager != null) {
            notificationManager.cancelAll()
        } else {
            android.util.Log.e("MainActivity", "Failed to get NotificationManager")
        }

        // Cancel all scheduled alarms
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        taskList.forEach { task ->
            val intent = Intent(this, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                task.notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun scheduleNotification(task: Task) {
        if (!task.isNotificationEnabled) {
            android.util.Log.d("MainActivity", "Notification not enabled for task: ${task.title}")
            return
        }

        // Check if the task's due time is in the past
        val dueDateTime = task.dueDate.atTime(task.dueTime)
        val now = LocalDateTime.now()
        if (dueDateTime.isBefore(now)) {
            android.util.Log.d("MainActivity", "Skipping notification for past-due task: ${task.title}")
            return
        }

        android.util.Log.d("MainActivity", "Scheduling notification for task: ${task.title}, due at: $dueDateTime")

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("taskTitle", task.title)
            putExtra("notificationId", task.notificationId)
            putExtra("taskType", task.type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            task.notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate time 30 minutes before due date
        val calendar = Calendar.getInstance().apply {
            set(task.dueDate.year, task.dueDate.monthValue - 1, task.dueDate.dayOfMonth,
                task.dueTime.hour, task.dueTime.minute)
            add(Calendar.MINUTE, -30) // 30 minutes before
        }

        // Check if the app can schedule exact alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                try {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    Toast.makeText(this, "Unable to schedule alarm due to permissions.", Toast.LENGTH_LONG).show()
                }
            } else {
                // Prompt user to grant permission
                Toast.makeText(this, "Please enable exact alarm permission in settings.", Toast.LENGTH_LONG).show()
                val settingsIntent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(settingsIntent)
                return // Exit function until permission is granted
            }
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun removeTask(task: Task) {
        val index = taskList.indexOf(task)
        if (index != -1) {
            // Cancel notification when task is removed
            cancelNotification(task)
            taskList.removeAt(index)
            taskAdapter.notifyItemRemoved(index)
            saveTasks()
        }
    }

    private fun cancelNotification(task: Task) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            task.notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun saveTasks() {
        val sharedPreferences = getSharedPreferences("tasks", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val taskStrings = taskList.map { it.toFormattedString() }
        editor.putStringSet("taskList", taskStrings.toSet())
        editor.apply()
    }

    private fun loadTasks() {
        val sharedPreferences = getSharedPreferences("tasks", Context.MODE_PRIVATE)
        // Clear SharedPreferences
        // sharedPreferences.edit().clear().apply()

        val taskStrings = sharedPreferences.getStringSet("taskList", emptySet()) ?: emptySet()
        taskList.addAll(taskStrings.map { Task.fromFormattedString(it) })
        taskList.forEach { task ->
            android.util.Log.d("MainActivity", "Loaded task: ${task.title}, isNotificationEnabled: ${task.isNotificationEnabled}")
        }
        taskList.forEach { scheduleNotification(it) }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                // Reschedule notifications if permission is now granted
                taskList.forEach { scheduleNotification(it) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Reschedule all notifications when app is closed
        taskList.forEach { scheduleNotification(it) }
    }

    // TaskAdapter for RecyclerView
    class TaskAdapter(private val taskList: MutableList<Task>, private val onTaskRemoved: (Task) -> Unit) :
        RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

        inner class TaskViewHolder(val binding: TaskItemBinding) : RecyclerView.ViewHolder(binding.root) {
            init {
                binding.removeTaskCheckBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        val task = itemView.tag as Task
                        onTaskRemoved(task)
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val binding = TaskItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return TaskViewHolder(binding)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            val task = taskList[position]
            holder.binding.taskTitleTextView.text = task.title
            holder.binding.taskDescriptionTextView.text = task.description
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val formattedDateTime = task.dueDate.atTime(task.dueTime).format(formatter)
            holder.binding.taskDueDateTextView.text = "Due: $formattedDateTime"
            holder.itemView.tag = task
            holder.binding.removeTaskCheckBox.isChecked = false

            // Set title color based on task type
            when (task.type) {
                "Assignment" -> holder.binding.taskTitleTextView.setTextColor(Color.RED)
                "Task" -> holder.binding.taskTitleTextView.setTextColor(Color.BLUE)
                else -> holder.binding.taskTitleTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black)) // Default color
            }
        }

        override fun getItemCount(): Int = taskList.size
    }
}