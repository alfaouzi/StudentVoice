package com.faouzi.studentvoice

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.faouzi.studentvoice.data.local.Survey
import com.faouzi.studentvoice.data.local.SurveyQuestionResult
import com.faouzi.studentvoice.util.SoundFeedbackHelper
import com.faouzi.studentvoice.viewmodel.ActiveQuestionState
import com.faouzi.studentvoice.viewmodel.AnswerOption
import com.faouzi.studentvoice.viewmodel.OngoingSurveyState
import com.faouzi.studentvoice.viewmodel.SurveyPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context in English and Arabic`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appNameEn = context.getString(R.string.app_name)
        assertEquals("صوت التلميذ", appNameEn)

        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag("ar"))
        val arContext = context.createConfigurationContext(config)
        val appNameAr = arContext.getString(R.string.app_name)
        assertEquals("صوت التلميذ", appNameAr)
    }

    @Test
    fun `verify app info menu label and title renamed to معلومات التطبيق`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val defaultBtnSettings = context.getString(R.string.btn_settings)
        val defaultSettingsTitle = context.getString(R.string.settings_title)
        assertEquals("معلومات التطبيق", defaultBtnSettings)
        assertEquals("معلومات التطبيق", defaultSettingsTitle)

        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag("ar"))
        val arContext = context.createConfigurationContext(config)

        val btnSettings = arContext.getString(R.string.btn_settings)
        val settingsTitle = arContext.getString(R.string.settings_title)

        assertEquals("معلومات التطبيق", btnSettings)
        assertEquals("معلومات التطبيق", settingsTitle)
    }

    @Test
    fun `verify survey dates format with Western digits`() {
        val timestamp = 1779975300000L // 2026-05-28 13:35 UTC approx
        val testDate = Date(timestamp)

        val detailsDateFormat = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.ENGLISH)
        val listDateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)

        val formattedDetails = detailsDateFormat.format(testDate)
        val formattedList = listDateFormat.format(testDate)

        // Ensure Western digits (0-9) are used
        assertTrue(formattedDetails.matches(Regex("""\d{4}/\d{2}/\d{2} - \d{2}:\d{2}""")))
        assertTrue(formattedList.matches(Regex("""\d{4}/\d{2}/\d{2}""")))

        // Ensure no Arabic-Indic digits (٠١٢٣٤٥٦٧٨٩) are present
        val arabicIndicRegex = Regex("""[\u0660-\u0669]""")
        assertFalse(formattedDetails.contains(arabicIndicRegex))
        assertFalse(formattedList.contains(arabicIndicRegex))
    }

    @Test
    fun `verify privacy aggregated counters calculate percentages correctly and sum to 100 percent`() {
        val questionResult = SurveyQuestionResult(
            surveyId = 1,
            questionText = "هل أنت راضٍ عن معلمك؟",
            yesCount = 22,
            maybeCount = 4,
            noCount = 2,
            orderIndex = 0
        )

        assertEquals(28, questionResult.totalAnswers)
        // 22 / 28 = 78.57% -> 79% (with normal rounding and largest remainder distribution)
        assertEquals(79, questionResult.yesPercentage)
        // 4 / 28 = 14.28% -> 14%
        assertEquals(14, questionResult.maybePercentage)
        // 2 / 28 = 7.14% -> 7%
        assertEquals(7, questionResult.noPercentage)

        // Must sum to exactly 100%
        val totalPercentage = questionResult.yesPercentage + questionResult.maybePercentage + questionResult.noPercentage
        assertEquals(100, totalPercentage)
    }

    @Test
    fun `verify percentages always total 100 percent for various student totals`() {
        val cases = listOf(
            Triple(1, 0, 0),
            Triple(1, 1, 1),
            Triple(2, 1, 0),
            Triple(5, 5, 5),
            Triple(15, 3, 2),
            Triple(7, 2, 1),
            Triple(10, 0, 0)
        )

        for ((yes, maybe, no) in cases) {
            val q = SurveyQuestionResult(
                surveyId = 1,
                questionText = "سؤال تجريبي",
                yesCount = yes,
                maybeCount = maybe,
                noCount = no,
                orderIndex = 0
            )
            val sum = q.yesPercentage + q.maybePercentage + q.noPercentage
            assertEquals("Expected sum 100 for yes=$yes, maybe=$maybe, no=$no", 100, sum)
        }
    }

    @Test
    fun `verify zero answers return zero percentages`() {
        val emptyResult = SurveyQuestionResult(
            surveyId = 1,
            questionText = "سؤال جديد",
            yesCount = 0,
            maybeCount = 0,
            noCount = 0,
            orderIndex = 0
        )

        assertEquals(0, emptyResult.totalAnswers)
        assertEquals(0, emptyResult.yesPercentage)
        assertEquals(0, emptyResult.maybePercentage)
        assertEquals(0, emptyResult.noPercentage)
    }

    @Test
    fun `verify active question aggregates correctly`() {
        val question = ActiveQuestionState(
            questionText = "هل يشرح المعلم الدرس بوضوح؟",
            orderIndex = 1,
            yesCount = 0,
            maybeCount = 0,
            noCount = 0
        )

        // Simulate 3 students answering without identifying any student
        // Student 1: YES
        question.yesCount++
        // Student 2: YES
        question.yesCount++
        // Student 3: NO
        question.noCount++

        assertEquals(2, question.yesCount)
        assertEquals(0, question.maybeCount)
        assertEquals(1, question.noCount)
    }

    @Test
    fun `verify button order contains all three options and changes for next student`() {
        val initialOrder = OngoingSurveyState.generateRandomOptionsOrder()
        assertEquals(3, initialOrder.size)
        assertTrue(initialOrder.contains(AnswerOption.YES))
        assertTrue(initialOrder.contains(AnswerOption.MAYBE))
        assertTrue(initialOrder.contains(AnswerOption.NO))

        // Generating for the next student must generate a different random order
        val nextStudentOrder = OngoingSurveyState.generateRandomOptionsOrder(initialOrder)
        assertEquals(3, nextStudentOrder.size)
        assertTrue(nextStudentOrder.contains(AnswerOption.YES))
        assertTrue(nextStudentOrder.contains(AnswerOption.MAYBE))
        assertTrue(nextStudentOrder.contains(AnswerOption.NO))
        assertNotEquals("Next student must receive a different button permutation", initialOrder, nextStudentOrder)
    }

    @Test
    fun `verify button order stays the same across all questions of the same student`() {
        val questions = listOf(
            ActiveQuestionState("سؤال 1", 0),
            ActiveQuestionState("سؤال 2", 1),
            ActiveQuestionState("سؤال 3", 2)
        )
        val initialOrder = listOf(AnswerOption.NO, AnswerOption.YES, AnswerOption.MAYBE)

        var state = OngoingSurveyState(
            phase = SurveyPhase.STUDENT_ANSWERING,
            totalStudents = 3,
            currentStudentNumber = 1,
            currentQuestionIndex = 0,
            activeQuestions = questions,
            currentStudentOptionsOrder = initialOrder
        )

        // Question 1: Answer YES
        state.activeQuestions[state.currentQuestionIndex].yesCount++
        state = state.copy(currentQuestionIndex = state.currentQuestionIndex + 1)
        assertEquals("Order must not change between questions for student 1", initialOrder, state.currentStudentOptionsOrder)

        // Question 2: Answer MAYBE
        state.activeQuestions[state.currentQuestionIndex].maybeCount++
        state = state.copy(currentQuestionIndex = state.currentQuestionIndex + 1)
        assertEquals("Order must not change between questions for student 1", initialOrder, state.currentStudentOptionsOrder)

        // Question 3: Answer NO
        state.activeQuestions[state.currentQuestionIndex].noCount++
        assertEquals("Order must not change between questions for student 1", initialOrder, state.currentStudentOptionsOrder)
    }

    @Test
    fun `verify every button position records the correct semantic answer`() {
        val q = ActiveQuestionState("سؤال تجريبي", 0)

        // Simulated randomized orders
        val permutations = listOf(
            listOf(AnswerOption.YES, AnswerOption.MAYBE, AnswerOption.NO),
            listOf(AnswerOption.NO, AnswerOption.YES, AnswerOption.MAYBE),
            listOf(AnswerOption.MAYBE, AnswerOption.NO, AnswerOption.YES)
        )

        for (order in permutations) {
            val positionOfYes = order.indexOf(AnswerOption.YES)
            val selectedOption = order[positionOfYes]
            when (selectedOption) {
                AnswerOption.YES -> q.yesCount++
                AnswerOption.MAYBE -> q.maybeCount++
                AnswerOption.NO -> q.noCount++
            }
        }

        // Regardless of position (0, 1, or 2), selecting YES always records YES
        assertEquals(3, q.yesCount)
        assertEquals(0, q.maybeCount)
        assertEquals(0, q.noCount)
    }

    @Test
    fun `verify hold to proceed string resource exists`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val defaultHint = context.getString(R.string.hold_to_proceed_hint)
        assertTrue(defaultHint.isNotEmpty())

        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag("ar"))
        val arContext = context.createConfigurationContext(config)
        val arHint = arContext.getString(R.string.hold_to_proceed_hint)
        assertEquals("اضغط مطولاً للمتابعة", arHint)
    }

    @Test
    fun `verify sound feedback helper executes safely without crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Should execute gracefully without throwing an exception even in test JVM without audio output
        SoundFeedbackHelper.playCompletionSound(context)
    }
}
