package com.faouzi.studentvoice.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faouzi.studentvoice.data.local.AppDatabase
import com.faouzi.studentvoice.data.local.Survey
import com.faouzi.studentvoice.data.local.SurveyQuestionResult
import com.faouzi.studentvoice.data.local.SurveyRepository
import com.faouzi.studentvoice.data.local.SurveyWithResults
import com.faouzi.studentvoice.util.SoundFeedbackHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AnswerOption {
    YES,
    MAYBE,
    NO
}

enum class SurveyPhase {
    SETUP,
    STUDENT_ANSWERING,
    STUDENT_COMPLETED,
    SURVEY_COMPLETED
}

data class ActiveQuestionState(
    val questionText: String,
    val orderIndex: Int,
    var yesCount: Int = 0,
    var maybeCount: Int = 0,
    var noCount: Int = 0
)

data class OngoingSurveyState(
    val phase: SurveyPhase = SurveyPhase.SETUP,
    val teacherName: String = "",
    val className: String = "",
    val subject: String = "",
    val totalStudents: Int = 0,
    val currentStudentNumber: Int = 1,
    val currentQuestionIndex: Int = 0,
    val activeQuestions: List<ActiveQuestionState> = emptyList(),
    val savedSurveyId: Long? = null,
    val isSaving: Boolean = false,
    val currentStudentOptionsOrder: List<AnswerOption> = defaultOptionsOrder()
) {
    companion object {
        fun defaultOptionsOrder(): List<AnswerOption> =
            listOf(AnswerOption.YES, AnswerOption.MAYBE, AnswerOption.NO)

        fun generateRandomOptionsOrder(previousOrder: List<AnswerOption>? = null): List<AnswerOption> {
            val base = listOf(AnswerOption.YES, AnswerOption.MAYBE, AnswerOption.NO)
            var shuffled = base.shuffled()
            if (previousOrder != null && previousOrder.size > 1) {
                var attempts = 0
                while (shuffled == previousOrder && attempts < 10) {
                    shuffled = base.shuffled()
                    attempts++
                }
            }
            return shuffled
        }
    }

    val currentQuestion: ActiveQuestionState?
        get() = activeQuestions.getOrNull(currentQuestionIndex)

    val totalQuestions: Int
        get() = activeQuestions.size

    val isLastStudent: Boolean
        get() = currentStudentNumber >= totalStudents

    val isLastQuestionForStudent: Boolean
        get() = currentQuestionIndex >= (totalQuestions - 1)
}

class SurveyViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: SurveyRepository = SurveyRepository(AppDatabase.getInstance(application).surveyDao())
) : AndroidViewModel(application) {

    val allSurveys: StateFlow<List<SurveyWithResults>> = repository.allSurveys
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _surveyState = MutableStateFlow(OngoingSurveyState())
    val surveyState: StateFlow<OngoingSurveyState> = _surveyState.asStateFlow()

    // For viewing an archived/saved survey results
    private val _selectedArchivedSurvey = MutableStateFlow<SurveyWithResults?>(null)
    val selectedArchivedSurvey: StateFlow<SurveyWithResults?> = _selectedArchivedSurvey.asStateFlow()

    // Guard ensuring the completion beep plays exactly once per completed student
    private var lastSoundPlayedStudentNumber: Int = -1

    fun startNewSurvey(
        teacherName: String,
        className: String,
        subject: String,
        studentCount: Int,
        questions: List<String>
    ) {
        lastSoundPlayedStudentNumber = -1
        val activeQuestions = questions.mapIndexed { index, text ->
            ActiveQuestionState(
                questionText = text,
                orderIndex = index,
                yesCount = 0,
                maybeCount = 0,
                noCount = 0
            )
        }

        _surveyState.value = OngoingSurveyState(
            phase = SurveyPhase.STUDENT_ANSWERING,
            teacherName = teacherName.trim(),
            className = className.trim(),
            subject = subject.trim(),
            totalStudents = studentCount,
            currentStudentNumber = 1,
            currentQuestionIndex = 0,
            activeQuestions = activeQuestions,
            savedSurveyId = null,
            currentStudentOptionsOrder = OngoingSurveyState.generateRandomOptionsOrder()
        )
    }

    fun recordAnswer(answer: AnswerOption) {
        val current = _surveyState.value
        val currentQIndex = current.currentQuestionIndex
        if (currentQIndex !in current.activeQuestions.indices) return

        // Update ONLY aggregated counter for privacy
        val targetQuestion = current.activeQuestions[currentQIndex]
        when (answer) {
            AnswerOption.YES -> targetQuestion.yesCount++
            AnswerOption.MAYBE -> targetQuestion.maybeCount++
            AnswerOption.NO -> targetQuestion.noCount++
        }

        if (current.isLastQuestionForStudent) {
            // Guarded: play one short gentle beep exactly once when this student completes the last question
            val studentNum = current.currentStudentNumber
            if (lastSoundPlayedStudentNumber != studentNum) {
                lastSoundPlayedStudentNumber = studentNum
                SoundFeedbackHelper.playCompletionSound(getApplication())
            }

            // Last question for this student -> go to Handover screen
            if (current.isLastStudent) {
                _surveyState.update {
                    it.copy(phase = SurveyPhase.SURVEY_COMPLETED)
                }
            } else {
                _surveyState.update {
                    it.copy(phase = SurveyPhase.STUDENT_COMPLETED)
                }
            }
        } else {
            // Move to next question for the same student
            _surveyState.update {
                it.copy(currentQuestionIndex = currentQIndex + 1)
            }
        }
    }

    fun proceedToNextStudent() {
        val current = _surveyState.value
        if (current.currentStudentNumber < current.totalStudents) {
            val newOrder = OngoingSurveyState.generateRandomOptionsOrder(current.currentStudentOptionsOrder)
            _surveyState.update {
                it.copy(
                    phase = SurveyPhase.STUDENT_ANSWERING,
                    currentStudentNumber = it.currentStudentNumber + 1,
                    currentQuestionIndex = 0,
                    currentStudentOptionsOrder = newOrder
                )
            }
        } else {
            _surveyState.update {
                it.copy(phase = SurveyPhase.SURVEY_COMPLETED)
            }
        }
    }

    fun completeAndSaveSurvey(onSaved: (Long) -> Unit) {
        val current = _surveyState.value
        if (current.isSaving) return

        _surveyState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val survey = Survey(
                    teacherName = current.teacherName,
                    className = current.className,
                    subject = current.subject,
                    studentCount = current.totalStudents,
                    dateCreated = System.currentTimeMillis(),
                    dateCompleted = System.currentTimeMillis()
                )

                val questionResults = current.activeQuestions.map { q ->
                    SurveyQuestionResult(
                        questionText = q.questionText,
                        yesCount = q.yesCount,
                        maybeCount = q.maybeCount,
                        noCount = q.noCount,
                        orderIndex = q.orderIndex
                    )
                }

                val savedId = repository.saveCompletedSurvey(survey, questionResults)
                val savedSurveyWithResults = repository.getSurveyById(savedId)
                _selectedArchivedSurvey.value = savedSurveyWithResults
                _surveyState.update {
                    it.copy(
                        savedSurveyId = savedId
                    )
                }
                onSaved(savedId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("SurveyViewModel", "Failed to save completed survey", e)
            } finally {
                _surveyState.update {
                    it.copy(isSaving = false)
                }
            }
        }
    }

    fun selectArchivedSurvey(survey: SurveyWithResults) {
        _selectedArchivedSurvey.value = survey
    }

    fun loadSurveyById(surveyId: Long) {
        viewModelScope.launch {
            val found = repository.getSurveyById(surveyId)
            _selectedArchivedSurvey.value = found
        }
    }

    fun deleteSurvey(surveyId: Long) {
        viewModelScope.launch {
            repository.deleteSurvey(surveyId)
            if (_selectedArchivedSurvey.value?.survey?.id == surveyId) {
                _selectedArchivedSurvey.value = null
            }
        }
    }

    fun resetSurvey() {
        lastSoundPlayedStudentNumber = -1
        _surveyState.value = OngoingSurveyState()
    }
}
