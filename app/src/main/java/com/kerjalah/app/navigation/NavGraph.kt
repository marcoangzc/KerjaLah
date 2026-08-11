package com.kerjalah.app.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.kerjalah.app.data.data.UserRole
import com.kerjalah.app.ui.application.ApplicationDetailScreen
import com.kerjalah.app.ui.application.MyApplicationsScreen
import com.kerjalah.app.ui.employer.ApplicantDetailScreen
import com.kerjalah.app.ui.employer.ApplicantsScreen
import com.kerjalah.app.ui.employer.MyPostingsScreen
import com.kerjalah.app.ui.employer.PostJobScreen
import com.kerjalah.app.ui.job.JobDetailScreen
import com.kerjalah.app.ui.job.JobListScreen
import com.kerjalah.app.ui.user.EditProfileScreen
import com.kerjalah.app.ui.user.LoginScreen
import com.kerjalah.app.ui.user.ProfileScreen
import com.kerjalah.app.ui.user.RegisterScreen
import com.kerjalah.app.ui.user.RoleScreen
import com.kerjalah.app.ui.user.SplashScreen

// [A] One bottom-nav / rail tab.
private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

// [A] Tab sets per role. Applicants gets its entry point from a posting
// (it needs a jobId), so it is not a plain tab here; it will hook in
// with Module 3.
private val studentTabs = listOf(
    TabItem(Routes.STUDENT_JOBS, "Jobs", Icons.Filled.Search),
    TabItem(Routes.STUDENT_APPLICATIONS, "Applications", Icons.AutoMirrored.Filled.List),
    TabItem(Routes.STUDENT_PROFILE, "Profile", Icons.Filled.Person),
)
private val employerTabs = listOf(
    TabItem(Routes.EMPLOYER_POSTINGS, "Postings", Icons.AutoMirrored.Filled.List),
    TabItem(Routes.EMPLOYER_PROFILE, "Profile", Icons.Filled.Person),
)

// [A] The single navigation map of the app.
// Screens NEVER hold a NavController: they emit lambda events,
// and only this file decides where to go (UDF for navigation).
@Composable
fun NavGraph(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Which tab bar to show (null = no tabs, e.g. auth screens / details).
    val tabs = when {
        studentTabs.any { it.route == currentRoute } -> studentTabs
        employerTabs.any { it.route == currentRoute } -> employerTabs
        else -> null
    }

    fun onTabClick(tabs: List<TabItem>, tab: TabItem) {
        navController.navigate(tab.route) {
            // Keep ONE copy of each tab on the stack; save/restore its state.
            popUpTo(tabs.first().route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun goHome(role: UserRole) =
        if (role == UserRole.STUDENT) Routes.STUDENT_JOBS else Routes.EMPLOYER_POSTINGS

    // Phone: bottom NavigationBar. Tablet (>= 600dp wide): side NavigationRail.
    BoxWithConstraints {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            bottomBar = {
                if (tabs != null && !useRail) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = { onTabClick(tabs, tab) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (tabs != null && useRail) {
                    NavigationRail {
                        tabs.forEach { tab ->
                            NavigationRailItem(
                                selected = currentRoute == tab.route,
                                onClick = { onTabClick(tabs, tab) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.SPLASH,
                    modifier = Modifier.weight(1f),
                ) {
                    // ---------- Entry flow (Module 1) ----------

                    composable(Routes.SPLASH) {
                        SplashScreen(
                            onDone = { role ->
                                val target = role?.let { goHome(it) } ?: Routes.LOGIN
                                navController.navigate(target) {
                                    // Splash never stays on the back stack.
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable(Routes.LOGIN) {
                        LoginScreen(
                            onLoginSuccess = { role ->
                                navController.navigate(goHome(role)) {
                                    // Clear the auth flow after login.
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            },
                            onRegisterClick = {
                                navController.navigate(Routes.REGISTER)
                            },
                        )
                    }

                    composable(Routes.REGISTER) {
                        RegisterScreen(
                            onBackClick = { navController.popBackStack() },
                            onContinueToRole = { navController.navigate(Routes.ROLE) },
                        )
                    }

                    composable(Routes.ROLE) {
                        RoleScreen(
                            onRoleConfirmed = { role ->
                                navController.navigate(goHome(role)) {
                                    // Clear login/register/role after signup.
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            },
                        )
                    }

                    // ---------- Student side ----------

                    composable(Routes.STUDENT_JOBS) {
                        JobListScreen(
                            onJobClick = { jobId ->
                                navController.navigate(Routes.studentJobDetail(jobId))
                            },
                        )
                    }

                    composable(
                        route = Routes.STUDENT_JOB_DETAIL,
                        arguments = listOf(navArgument("jobId") { type = NavType.StringType }),
                    ) { entry ->
                        val jobId = entry.arguments?.getString("jobId") ?: return@composable
                        JobDetailScreen(
                            jobId = jobId,
                            onBackClick = { navController.popBackStack() },
                            // Apply is now a real ViewModel event inside the
                            // screen (Module 3) - no navigation needed here.
                        )
                    }

                    composable(Routes.STUDENT_APPLICATIONS) {
                        MyApplicationsScreen(
                            onApplicationClick = { appId ->
                                navController.navigate(Routes.studentApplicationDetail(appId))
                            },
                        )
                    }
                    composable(
                        route = Routes.STUDENT_APPLICATION_DETAIL,
                        arguments = listOf(navArgument("appId") { type = NavType.StringType }),
                    ) { entry ->
                        val appId = entry.arguments?.getString("appId") ?: return@composable
                        ApplicationDetailScreen(
                            appId = appId,
                            onBackClick = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.STUDENT_PROFILE) {
                        ProfileScreen(
                            onEditClick = {
                                navController.navigate(Routes.STUDENT_PROFILE_EDIT)
                            },
                            onLoggedOut = {
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(0) { inclusive = true } // wipe everything
                                }
                            },
                        )
                    }
                    composable(Routes.STUDENT_PROFILE_EDIT) {
                        EditProfileScreen(
                            onBackClick = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() },
                        )
                    }

                    // ---------- Employer side ----------

                    composable(Routes.EMPLOYER_POSTINGS) {
                        MyPostingsScreen(
                            onAddClick = {
                                navController.navigate(Routes.employerPost())
                            },
                            onEditClick = { jobId ->
                                navController.navigate(Routes.employerPost(jobId))
                            },
                            onApplicantsClick = { jobId ->
                                navController.navigate(Routes.employerApplicants(jobId))
                            },
                        )
                    }

                    composable(
                        route = Routes.EMPLOYER_POST,
                        arguments = listOf(
                            // Optional query argument: null = post new job.
                            navArgument("jobId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
                    ) { entry ->
                        val jobId = entry.arguments?.getString("jobId")
                        PostJobScreen(
                            jobId = jobId,
                            onBackClick = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() },
                        )
                    }

                    composable(
                        route = Routes.EMPLOYER_APPLICANTS,
                        arguments = listOf(navArgument("jobId") { type = NavType.StringType }),
                    ) { entry ->
                        val jobId = entry.arguments?.getString("jobId") ?: return@composable
                        ApplicantsScreen(
                            jobId = jobId,
                            onBackClick = { navController.popBackStack() },
                            onApplicantClick = { appId ->
                                navController.navigate(Routes.employerApplicantDetail(appId))
                            },
                        )
                    }
                    composable(
                        route = Routes.EMPLOYER_APPLICANT_DETAIL,
                        arguments = listOf(navArgument("appId") { type = NavType.StringType }),
                    ) { entry ->
                        val appId = entry.arguments?.getString("appId") ?: return@composable
                        ApplicantDetailScreen(
                            appId = appId,
                            onBackClick = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.EMPLOYER_PROFILE) {
                        ProfileScreen(
                            onEditClick = {
                                navController.navigate(Routes.EMPLOYER_PROFILE_EDIT)
                            },
                            onLoggedOut = {
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }
                    composable(Routes.EMPLOYER_PROFILE_EDIT) {
                        EditProfileScreen(
                            onBackClick = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
