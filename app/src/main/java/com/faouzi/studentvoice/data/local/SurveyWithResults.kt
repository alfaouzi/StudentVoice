package com.faouzi.studentvoice.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class SurveyWithResults(
    @Embedded val survey: Survey,
    @Relation(
        parentColumn = "id",
        entityColumn = "surveyId"
    )
    val questions: List<SurveyQuestionResult>
) {
    val sortedQuestions: List<SurveyQuestionResult>
        get() = questions.sortedBy { it.orderIndex }
}
