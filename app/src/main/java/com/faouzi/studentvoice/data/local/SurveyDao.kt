package com.faouzi.studentvoice.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurvey(survey: Survey): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionResults(results: List<SurveyQuestionResult>)

    @Transaction
    suspend fun insertSurveyWithResults(
        survey: Survey,
        questionResults: List<SurveyQuestionResult>
    ): Long {
        val surveyId = insertSurvey(survey)
        val linkedResults = questionResults.map { result ->
            result.copy(surveyId = surveyId)
        }
        insertQuestionResults(linkedResults)
        return surveyId
    }

    @Transaction
    @Query("SELECT * FROM surveys ORDER BY dateCompleted DESC")
    fun getAllSurveysWithResults(): Flow<List<SurveyWithResults>>

    @Transaction
    @Query("SELECT * FROM surveys WHERE id = :surveyId")
    suspend fun getSurveyWithResultsById(surveyId: Long): SurveyWithResults?

    @Query("DELETE FROM surveys WHERE id = :surveyId")
    suspend fun deleteSurveyById(surveyId: Long)
}
