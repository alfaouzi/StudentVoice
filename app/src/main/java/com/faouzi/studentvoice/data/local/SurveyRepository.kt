package com.faouzi.studentvoice.data.local

import kotlinx.coroutines.flow.Flow

class SurveyRepository(private val surveyDao: SurveyDao) {

    val allSurveys: Flow<List<SurveyWithResults>> = surveyDao.getAllSurveysWithResults()

    suspend fun saveCompletedSurvey(
        survey: Survey,
        questionResults: List<SurveyQuestionResult>
    ): Long {
        val surveyId = surveyDao.insertSurvey(survey)
        val linkedResults = questionResults.map { result ->
            result.copy(surveyId = surveyId)
        }
        surveyDao.insertQuestionResults(linkedResults)
        return surveyId
    }

    suspend fun getSurveyById(surveyId: Long): SurveyWithResults? {
        return surveyDao.getSurveyWithResultsById(surveyId)
    }

    suspend fun deleteSurvey(surveyId: Long) {
        surveyDao.deleteSurveyById(surveyId)
    }
}
