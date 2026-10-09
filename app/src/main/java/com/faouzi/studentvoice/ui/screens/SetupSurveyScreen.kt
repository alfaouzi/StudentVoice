package com.faouzi.studentvoice.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faouzi.studentvoice.R

data class QuestionItem(
    val id: Int,
    val text: String,
    val isSelected: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupSurveyScreen(
    onNavigateBack: () -> Unit,
    onStartSurvey: (teacherName: String, className: String, subject: String, studentCount: Int, questions: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var teacherName by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var studentCountText by remember { mutableStateOf("") }
    var studentCountError by remember { mutableStateOf<String?>(null) }
    var questionsError by remember { mutableStateOf<String?>(null) }

    val defaultQuestions = listOf(
        stringResource(R.string.default_q1),
        stringResource(R.string.default_q2),
        stringResource(R.string.default_q3),
        stringResource(R.string.default_q4),
        stringResource(R.string.default_q5),
        stringResource(R.string.default_q6)
    )

    val questionsList = remember {
        mutableStateListOf<QuestionItem>().apply {
            addAll(defaultQuestions.mapIndexed { idx, q -> QuestionItem(id = idx, text = q, isSelected = true) })
        }
    }

    var isAddingCustomQuestion by remember { mutableStateOf(false) }
    var customQuestionInput by remember { mutableStateOf("") }

    val errCountMsg = stringResource(R.string.error_invalid_student_count)
    val errQuestionMsg = stringResource(R.string.error_no_question_selected)

    fun validateAndProceed() {
        val count = studentCountText.trim().toIntOrNull()
        var valid = true

        if (count == null || count <= 0) {
            studentCountError = errCountMsg
            valid = false
        } else {
            studentCountError = null
        }

        val selectedQuestions = questionsList.filter { it.isSelected }.map { it.text }
        if (selectedQuestions.isEmpty()) {
            questionsError = errQuestionMsg
            valid = false
        } else {
            questionsError = null
        }

        if (valid && count != null) {
            onStartSurvey(teacherName, className, subject, count, selectedQuestions)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.setup_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    if (studentCountError != null) {
                        Text(
                            text = studentCountError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    if (questionsError != null) {
                        Text(
                            text = questionsError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Button(
                        onClick = { validateAndProceed() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_survey_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.btn_start_survey),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // General Information Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "معلومات الاستطلاع",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Teacher Name (Optional)
                        OutlinedTextField(
                            value = teacherName,
                            onValueChange = { teacherName = it },
                            label = { Text(stringResource(R.string.label_teacher_name)) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("teacher_name_input")
                        )

                        // Class Name (Optional)
                        OutlinedTextField(
                            value = className,
                            onValueChange = { className = it },
                            label = { Text(stringResource(R.string.label_class_name)) },
                            leadingIcon = {
                                Icon(Icons.Default.Class, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("class_name_input")
                        )

                        // Subject (Optional)
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text(stringResource(R.string.label_subject)) },
                            leadingIcon = {
                                Icon(Icons.Default.Book, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subject_input")
                        )

                        // Student Count (Mandatory positive integer)
                        OutlinedTextField(
                            value = studentCountText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) {
                                    studentCountText = input
                                    if (studentCountError != null) studentCountError = null
                                }
                            },
                            label = { Text(stringResource(R.string.label_student_count)) },
                            placeholder = { Text(stringResource(R.string.hint_student_count)) },
                            leadingIcon = {
                                Icon(Icons.Default.Group, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = studentCountError != null,
                            supportingText = if (studentCountError != null) {
                                { Text(studentCountError ?: "", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_count_input")
                        )
                    }
                }
            }

            // Questions Selection Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.select_questions_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.select_questions_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Questions Checklist
            itemsIndexed(questionsList, key = { _, item -> item.id }) { index, question ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val nextSelected = !question.isSelected
                            questionsList[index] = question.copy(isSelected = nextSelected)
                            if (questionsError != null && questionsList.any { it.isSelected }) {
                                questionsError = null
                            }
                        }
                        .testTag("question_item_$index"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (question.isSelected) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (question.isSelected) 2.dp else 0.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = question.isSelected,
                            onCheckedChange = { isChecked ->
                                questionsList[index] = question.copy(isSelected = isChecked)
                                if (questionsError != null && questionsList.any { it.isSelected }) {
                                    questionsError = null
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("checkbox_$index")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = question.text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (question.isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (question.isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Add Custom Question Section
            item {
                if (isAddingCustomQuestion) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = customQuestionInput,
                                onValueChange = { customQuestionInput = it },
                                placeholder = { Text(stringResource(R.string.custom_question_hint)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isAddingCustomQuestion = false
                                        customQuestionInput = ""
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(stringResource(R.string.cancel))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (customQuestionInput.isNotBlank()) {
                                            questionsList.add(
                                                QuestionItem(
                                                    id = questionsList.size + 100,
                                                    text = customQuestionInput.trim(),
                                                    isSelected = true
                                                )
                                            )
                                            customQuestionInput = ""
                                            isAddingCustomQuestion = false
                                            questionsError = null
                                        }
                                    },
                                    enabled = customQuestionInput.isNotBlank(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(stringResource(R.string.btn_add))
                                }
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { isAddingCustomQuestion = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.add_custom_question))
                    }
                }
            }
        }
    }
}
