package com.faouzi.studentvoice.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "survey_question_results",
    foreignKeys = [
        ForeignKey(
            entity = Survey::class,
            parentColumns = ["id"],
            childColumns = ["surveyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("surveyId")]
)
data class SurveyQuestionResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val surveyId: Long = 0,
    val questionText: String,
    val yesCount: Int = 0,
    val maybeCount: Int = 0,
    val noCount: Int = 0,
    val orderIndex: Int = 0
) {
    val totalAnswers: Int
        get() = yesCount + maybeCount + noCount

    val yesPercentage: Int
        get() = if (totalAnswers > 0) (yesCount * 100 / totalAnswers) else 0

    val maybePercentage: Int
        get() = if (totalAnswers > 0) (maybeCount * 100 / totalAnswers) else 0

    val noPercentage: Int
        get() = if (totalAnswers > 0) (noCount * 100 / totalAnswers) else 0
}
