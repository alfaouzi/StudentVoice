package com.faouzi.studentvoice.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faouzi.studentvoice.R
import com.faouzi.studentvoice.ui.theme.AnswerMaybe
import com.faouzi.studentvoice.ui.theme.AnswerNo
import com.faouzi.studentvoice.ui.theme.AnswerYes
import com.faouzi.studentvoice.util.SoundFeedbackHelper
import com.faouzi.studentvoice.viewmodel.AnswerOption
import com.faouzi.studentvoice.viewmodel.OngoingSurveyState
import com.faouzi.studentvoice.viewmodel.SurveyPhase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StudentSurveyScreen(
    surveyState: OngoingSurveyState,
    onAnswer: (AnswerOption) -> Unit,
    onNextStudent: () -> Unit,
    onShowResults: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (surveyState.phase) {
                SurveyPhase.STUDENT_ANSWERING -> {
                    StudentAnsweringContent(
                        surveyState = surveyState,
                        onAnswer = onAnswer
                    )
                }

                SurveyPhase.STUDENT_COMPLETED -> {
                    StudentCompletedHandoverContent(
                        surveyState = surveyState,
                        onNextStudent = onNextStudent
                    )
                }

                SurveyPhase.SURVEY_COMPLETED -> {
                    SurveyFinishedContent(
                        surveyState = surveyState,
                        onShowResults = onShowResults
                    )
                }

                else -> {
                    // Fallback loading or idle
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

/**
 * Screen presented to the student to answer the current question.
 * Strictly anonymous, highly visible, large buttons, no distraction.
 */
@Composable
private fun StudentAnsweringContent(
    surveyState: OngoingSurveyState,
    onAnswer: (AnswerOption) -> Unit
) {
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val currentQuestion = surveyState.currentQuestion
    val questionProgress = if (surveyState.totalQuestions > 0) {
        (surveyState.currentQuestionIndex + 1).toFloat() / surveyState.totalQuestions.toFloat()
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header: Session & Question Progress
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Student Session Counter
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.testTag("student_session_badge")
                ) {
                    Text(
                        text = stringResource(
                            R.string.student_session_format,
                            surveyState.currentStudentNumber,
                            surveyState.totalStudents
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                // Question Progress Text
                Text(
                    text = stringResource(
                        R.string.question_progress_format,
                        surveyState.currentQuestionIndex + 1,
                        surveyState.totalQuestions
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("question_progress_text")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Smooth Progress Bar
            LinearProgressIndicator(
                progress = { questionProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center: Question Prominently Displayed
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .testTag("question_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentQuestion?.questionText ?: "",
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "QuestionAnimation"
                ) { targetText ->
                    Text(
                        text = targetText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp,
                        modifier = Modifier.testTag("question_text")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Three Randomized Large Answer Buttons with Identical Neutral Styling
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Helper function for debounce
            val submitAnswer: (AnswerOption) -> Unit = { answer ->
                if (!isSubmitting) {
                    isSubmitting = true
                    onAnswer(answer)
                    coroutineScope.launch {
                        delay(250)
                        isSubmitting = false
                    }
                }
            }

            val options = surveyState.currentStudentOptionsOrder.ifEmpty {
                OngoingSurveyState.defaultOptionsOrder()
            }
            options.forEach { option ->
                AnswerButton(
                    option = option,
                    enabled = !isSubmitting,
                    onClick = { submitAnswer(option) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.survey_privacy_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Screen shown right after an individual student answers the last question.
 * Prompts the student to return the phone to the inspector.
 */
@Composable
private fun StudentCompletedHandoverContent(
    surveyState: OngoingSurveyState,
    onNextStudent: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(surveyState.currentStudentNumber) {
        SoundFeedbackHelper.playCompletionSound(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top status badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.student_session_format,
                    surveyState.currentStudentNumber,
                    surveyState.totalStudents
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Center: Thank you & Handover message
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.thank_you),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.handover_instruction),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        // Bottom: Next Student Button (Inspector controlled: require ~1s long press)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HoldToProceedButton(
                onHoldComplete = onNextStudent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.hold_to_proceed_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Screen shown after the very last student completes their questions.
 */
@Composable
private fun SurveyFinishedContent(
    surveyState: OngoingSurveyState,
    onShowResults: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        SoundFeedbackHelper.playCompletionSound(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Center: All students completed celebration card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(AnswerYes.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AnswerYes,
                    modifier = Modifier.size(68.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.survey_finished),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.all_students_completed),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.students_count_format, surveyState.totalStudents),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Bottom: Show Results Button
        Button(
            onClick = onShowResults,
            enabled = !surveyState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("show_results_button"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (surveyState.isSaving) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.btn_show_results),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Button requiring a short long-press (~1 second) to advance to the next student.
 * Normal taps do not trigger transition. Displays progress while holding.
 */
@Composable
fun HoldToProceedButton(
    onHoldComplete: () -> Unit,
    modifier: Modifier = Modifier,
    holdDurationMs: Long = 1000L
) {
    val progress = remember { Animatable(0f) }
    var isPressed by remember { mutableStateOf(false) }
    val currentOnHoldComplete by rememberUpdatedState(onHoldComplete)

    LaunchedEffect(isPressed) {
        if (isPressed) {
            var completed = false
            try {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = holdDurationMs.toInt(),
                        easing = LinearEasing
                    )
                )
                if (progress.value >= 1f) {
                    completed = true
                    currentOnHoldComplete()
                }
            } finally {
                if (!completed) {
                    progress.snapTo(0f)
                }
            }
        } else {
            progress.snapTo(0f)
        }
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
            .testTag("next_student_button"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Subtle filling progress overlay while holding
            if (progress.value > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress.value)
                        .background(Color.White.copy(alpha = 0.22f))
                        .align(Alignment.CenterStart)
                )
            }

            // Button label and icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_next_student),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Subtle progress indicator at the bottom
            if (progress.value > 0f) {
                LinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }
        }
    }
}

/**
 * Large anonymous answer button with neutral styling.
 * Displays identical neutral color for YES, MAYBE, and NO options to protect student privacy.
 */
@Composable
private fun AnswerButton(
    option: AnswerOption,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val testTag = when (option) {
        AnswerOption.YES -> "answer_button_yes"
        AnswerOption.MAYBE -> "answer_button_maybe"
        AnswerOption.NO -> "answer_button_no"
    }

    val labelText = when (option) {
        AnswerOption.YES -> stringResource(R.string.answer_yes)
        AnswerOption.MAYBE -> stringResource(R.string.answer_maybe)
        AnswerOption.NO -> stringResource(R.string.answer_no)
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 2.dp,
            focusedElevation = 2.dp,
            hoveredElevation = 2.dp,
            disabledElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = labelText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
