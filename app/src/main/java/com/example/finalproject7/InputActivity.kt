package com.example.finalproject7

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import androidx.activity.ComponentActivity

// Task data class that we made parcelable to be transfed via intents
// contains type, title, description, due date, due time, notifs bool, and notif id properties
@Parcelize
data class Task(
    val type: String,
    val title: String,
    val description: String,
    val dueDate: LocalDate,
    val dueTime: LocalTime,
    var isNotificationEnabled: Boolean = false,
    var notificationId: Int = -1
) : Parcelable {
    // Converts tasks to formatted strings for storing them
    fun toFormattedString(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val formattedDateTime = dueDate.atTime(dueTime).format(formatter)
        return "$type|$title|$description|$formattedDateTime|$isNotificationEnabled|$notificationId"
    }

    // Converts the formatted string back to a task object
    companion object {
        fun fromFormattedString(formattedString: String): Task {
            val parts = formattedString.split("|")
            val type = parts[0]
            val title = parts[1]
            val description = parts[2]
            val dateTimeString = parts[3]
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val dateTime = LocalDate.parse(dateTimeString.substring(0,10)).atTime(LocalTime.parse(dateTimeString.substring(11)))
            val dueDate = dateTime.toLocalDate()
            val dueTime = dateTime.toLocalTime()
            val isNotificationEnabled = parts[4].toBoolean()
            val notificationId = parts[5].toInt()
            return Task(type, title, description, dueDate, dueTime, isNotificationEnabled, notificationId)
        }
    }
}

// The Input/Add activity. Contains many UI elements, checkboxes, edit texts, etc that enter the task object information
class InputActivity : ComponentActivity() {

    private lateinit var returnButton: Button
    private lateinit var assignmentCheckBox: CheckBox
    private lateinit var taskCheckBox: CheckBox
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var dateTimeEditText: EditText
    private lateinit var confirmButton: Button
    private lateinit var reminderCheckBox: CheckBox

    // Stores selected date and time in special data types from java.time import
    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedTime: LocalTime = LocalTime.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        // Initializes views
        returnButton = findViewById(R.id.returnButton)
        assignmentCheckBox = findViewById(R.id.checkBox4)
        taskCheckBox = findViewById(R.id.checkBox5)
        titleEditText = findViewById(R.id.editTextText2)
        descriptionEditText = findViewById(R.id.editTextText3)
        dateTimeEditText = findViewById(R.id.dateTimeEditText)
        confirmButton = findViewById(R.id.confirmButton)
        reminderCheckBox = findViewById(R.id.checkBoxReminder)


        // Home button
        returnButton.setOnClickListener {
            finish() // Close the activity and return
        }

        // Shows the date and time picker when selected
        dateTimeEditText.setOnClickListener {
            showDateTimePicker()
        }

        // Saves task to list when clicked
        confirmButton.setOnClickListener {
            addTask()
        }

        // These lines of code make it so the task can only be of one type
        // It will automatically deselect other selections when one of them is clicked
        assignmentCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                taskCheckBox.isChecked = false
            }
        }
        taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                assignmentCheckBox.isChecked = false
            }
        }
    }

    // Pops up the calendar ui element to allow the user to select a date
    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
                showTimePicker()
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    // Pops up the clock ui element to allow the user to pick a time
    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                selectedTime = LocalTime.of(selectedHour, selectedMinute)
                updateDateTimeEditText()
            },
            hour,
            minute,
            false
        )
        timePickerDialog.show()
    }

    // Updates the dateTime input field with corrected format
    private fun updateDateTimeEditText() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val formattedDateTime = selectedDate.atTime(selectedTime).format(formatter)
        dateTimeEditText.setText(formattedDateTime)
    }

    // Creates task object and sends it back to the calling activity
    private fun addTask() {
        val type = when {
            assignmentCheckBox.isChecked -> "Assignment"
            taskCheckBox.isChecked -> "Task"
            else -> {
                Toast.makeText(this, "Please select a type", Toast.LENGTH_SHORT).show()
                return
            }
        }
        val title = titleEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val task = Task(
            type,
            title,
            description,
            selectedDate,
            selectedTime,
            isNotificationEnabled = reminderCheckBox.isChecked
        )

        // returns the created task back to the previous activity
        val resultIntent = Intent()
        resultIntent.putExtra("newTask", task)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
// End of class