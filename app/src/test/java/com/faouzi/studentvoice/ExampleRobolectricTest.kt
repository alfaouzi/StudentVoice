package com.faouzi.studentvoice

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.faouzi.studentvoice.data.local.Survey
import com.faouzi.studentvoice.data.local.SurveyQuestionResult
import com.faouzi.studentvoice.viewmodel.ActiveQuestionState
import com.faouzi.studentvoice.viewmodel.AnswerOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context in English and Arabic`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appNameEn = context.getString(R.string.app_name)
        assertEquals("Student Voice", appNameEn)

        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale("ar"))
        val arContext = context.createConfigurationContext(config)
        val appNameAr = arContext.getString(R.string.app_name)
        assertEquals("صوت التلميذ", appNameAr)
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
}
