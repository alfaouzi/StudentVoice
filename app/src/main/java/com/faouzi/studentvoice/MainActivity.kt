package com.faouzi.studentvoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.faouzi.studentvoice.data.local.Survey
import com.faouzi.studentvoice.data.local.SurveyQuestionResult
import com.faouzi.studentvoice.ui.navigation.NavRoutes
import com.faouzi.studentvoice.ui.screens.HomeScreen
import com.faouzi.studentvoice.ui.screens.PreviousSurveysScreen
import com.faouzi.studentvoice.ui.screens.ResultsScreen
import com.faouzi.studentvoice.ui.screens.SettingsScreen
import com.faouzi.studentvoice.ui.screens.SetupSurveyScreen
import com.faouzi.studentvoice.ui.screens.StudentSurveyScreen
import com.faouzi.studentvoice.ui.theme.StudentVoiceTheme
import com.faouzi.studentvoice.viewmodel.SurveyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudentVoiceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        StudentVoiceApp()
                    }
                }
            }
        }
    }
}

@Composable
fun StudentVoiceApp(
    viewModel: SurveyViewModel = viewModel()
) {
    val navController = rememberNavController()
    val surveyState by viewModel.surveyState.collectAsStateWithLifecycle()
    val allSurveys by viewModel.allSurveys.collectAsStateWithLifecycle()
    val selectedArchivedSurvey by viewModel.selectedArchivedSurvey.collectAsStateWithLifecycle()

    var showCancelSurveyDialog by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME,
        modifier = Modifier.fillMaxSize()
    ) {
        // Home Screen
        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToNewSurvey = {
                    navController.navigate(NavRoutes.SETUP)
                },
                onNavigateToPreviousSurveys = {
                    navController.navigate(NavRoutes.PREVIOUS_SURVEYS)
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                }
            )
        }

        // Setup Survey Screen
        composable(NavRoutes.SETUP) {
            SetupSurveyScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartSurvey = { teacherName, className, subject, studentCount, questions ->
                    viewModel.startNewSurvey(
                        teacherName = teacherName,
                        className = className,
                        subject = subject,
                        studentCount = studentCount,
                        questions = questions
                    )
                    navController.navigate(NavRoutes.ACTIVE_SURVEY)
                }
            )
        }

        // Student Survey Screen (Active session)
        composable(NavRoutes.ACTIVE_SURVEY) {
            // Prevent accidental back navigation during survey session
            BackHandler {
                showCancelSurveyDialog = true
            }

            StudentSurveyScreen(
                surveyState = surveyState,
                onAnswer = { answer ->
                    viewModel.recordAnswer(answer)
                },
                onNextStudent = {
                    viewModel.proceedToNextStudent()
                },
                onShowResults = {
                    viewModel.completeAndSaveSurvey { _ ->
                        navController.navigate(NavRoutes.RESULTS) {
                            popUpTo(NavRoutes.HOME) { inclusive = false }
                        }
                    }
                }
            )

            if (showCancelSurveyDialog) {
                AlertDialog(
                    onDismissRequest = { showCancelSurveyDialog = false },
                    title = {
                        Text("إلغاء الاستطلاع الجاري", style = MaterialTheme.typography.titleMedium)
                    },
                    text = {
                        Text("هل أنت متأكد من رغبتك في إلغاء هذا الاستطلاع؟ ستفقد البيانات التي تم إدخالها حتى الآن.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showCancelSurveyDialog = false
                                viewModel.resetSurvey()
                                navController.popBackStack(NavRoutes.HOME, false)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("تأكيد الإلغاء")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCancelSurveyDialog = false }) {
                            Text("متابعة الاستطلاع")
                        }
                    }
                )
            }
        }

        // Results Screen
        composable(NavRoutes.RESULTS) {
            // Determine whether to display newly completed survey or an archived survey
            val archived = selectedArchivedSurvey
            val currentSurvey: Survey
            val currentQuestions: List<SurveyQuestionResult>

            if (archived != null) {
                currentSurvey = archived.survey
                currentQuestions = archived.sortedQuestions
            } else {
                currentSurvey = Survey(
                    teacherName = surveyState.teacherName,
                    className = surveyState.className,
                    subject = surveyState.subject,
                    studentCount = surveyState.totalStudents,
                    dateCompleted = System.currentTimeMillis()
                )
                currentQuestions = surveyState.activeQuestions.map { q ->
                    SurveyQuestionResult(
                        questionText = q.questionText,
                        yesCount = q.yesCount,
                        maybeCount = q.maybeCount,
                        noCount = q.noCount,
                        orderIndex = q.orderIndex
                    )
                }
            }

            ResultsScreen(
                survey = currentSurvey,
                questions = currentQuestions,
                onBackHome = {
                    navController.popBackStack(NavRoutes.HOME, false)
                }
            )
        }

        // Previous Surveys Screen
        composable(NavRoutes.PREVIOUS_SURVEYS) {
            PreviousSurveysScreen(
                surveys = allSurveys,
                onSelectSurvey = { surveyWithResults ->
                    viewModel.selectArchivedSurvey(surveyWithResults)
                    navController.navigate(NavRoutes.RESULTS)
                },
                onDeleteSurvey = { surveyId ->
                    viewModel.deleteSurvey(surveyId)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNewSurvey = {
                    navController.navigate(NavRoutes.SETUP) {
                        popUpTo(NavRoutes.HOME)
                    }
                }
            )
        }

        // Settings Screen
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
