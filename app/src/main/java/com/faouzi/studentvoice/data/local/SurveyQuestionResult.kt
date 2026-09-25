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

    /**
     * Calculates aggregated percentages across all participating students for this question.
     * Uses the Largest Remainder Method (Hamilton method) to ensure the three percentages
     * strictly total 100% (allowing for normal rounding) whenever totalAnswers > 0.
     */
    val percentages: Triple<Int, Int, Int>
        get() {
            if (totalAnswers <= 0) return Triple(0, 0, 0)

            val rawYes = (yesCount * 100.0) / totalAnswers
            val rawMaybe = (maybeCount * 100.0) / totalAnswers
            val rawNo = (noCount * 100.0) / totalAnswers

            val floorYes = rawYes.toInt()
            val floorMaybe = rawMaybe.toInt()
            val floorNo = rawNo.toInt()

            val remainderYes = rawYes - floorYes
            val remainderMaybe = rawMaybe - floorMaybe
            val remainderNo = rawNo - floorNo

            var resYes = floorYes
            var resMaybe = floorMaybe
            var resNo = floorNo

            var extra = 100 - (resYes + resMaybe + resNo)

            // Distribute remaining points to the options with the largest fractional remainders
            val remainders = listOf(
                Triple(0, remainderYes, yesCount),
                Triple(1, remainderMaybe, maybeCount),
                Triple(2, remainderNo, noCount)
            ).sortedWith(
                compareByDescending<Triple<Int, Double, Int>> { it.second }
                    .thenByDescending { it.third }
            )

            for (item in remainders) {
                if (extra > 0) {
                    when (item.first) {
                        0 -> resYes++
                        1 -> resMaybe++
                        2 -> resNo++
                    }
                    extra--
                }
            }

            return Triple(resYes, resMaybe, resNo)
        }

    val yesPercentage: Int
        get() = percentages.first

    val maybePercentage: Int
        get() = percentages.second

    val noPercentage: Int
        get() = percentages.third
}
