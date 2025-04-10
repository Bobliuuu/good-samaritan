package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.example.myapplication.screens.map.MapScreen
import com.example.myapplication.screens.gallery.FullScreenGalleryScreen
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.admin.adminhome.AdminHomeScreen
import com.example.myapplication.admin.adminincident.AdminIncidentScreen
import com.example.myapplication.admin.admintag.AdminTagScreen
import com.example.myapplication.screens.addincident.AddIncidentScreen
import com.example.myapplication.screens.incidentdetail.IncidentDetailScreen
import com.example.myapplication.screens.incidentdetail.IncidentDetailViewModel
import com.example.myapplication.screens.notification.NotificationScreen
import com.example.myapplication.ui.screens.contacts.AddContactScreen
import com.example.myapplication.screens.contact.ContactsScreen
import com.example.myapplication.navigation.BottomNavigationBar
import org.koin.androidx.compose.getViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.HomeActivity
import com.example.myapplication.admin.adminuser.AdminUserScreen
import com.example.myapplication.admin.adminuser.AdminUserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp(isAdmin: Boolean,
          viewModel: BottomNavViewModel = koinViewModel()
) {
    val navController: NavHostController = rememberNavController()
    val context = LocalContext.current
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

    LaunchedEffect(isAdmin) {
        viewModel.setAdmin(isAdmin)
    }

    Scaffold(
        topBar = ({
            TopAppBar(
                title = { Text("Good Samaritan", color = androidx.compose.ui.graphics.Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    if (isLoggedIn) {
                        TextButton(onClick = {
                            FirebaseAuth.getInstance().signOut()
                            val intent = Intent(context, HomeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                        }) {
                            Text("Log Out", color = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                }
            )
        }),
        bottomBar = {
            BottomNavigationBar(navController = navController, viewModel = viewModel)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (viewModel.isAdmin.value) "admin_home" else "map",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("map") {
                MapScreen(navController = navController)
            }
            composable("family") {
                ContactsScreen(navController = navController)
            }
            composable("alerts") {
                AddIncidentScreen(onIncidentAdded = { navController.navigate("map") })
            }
            composable("notifications") {
                NotificationScreen()
            }

            composable("incident_detail/{incidentId}?isAdmin={isAdmin}",
                arguments = listOf(
                    navArgument("incidentId") { type = NavType.StringType },
                    navArgument("isAdmin") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val incidentId = backStackEntry.arguments?.getString("incidentId") ?: return@composable
                backStackEntry.arguments?.getBoolean("isAdmin") ?: false

                val incidentViewModel: IncidentDetailViewModel = getViewModel(parameters = { parametersOf(incidentId) })

                IncidentDetailScreen(
                    viewModel = incidentViewModel,
                    onBackPressed = { navController.popBackStack() },
                    onImageClick = { index ->
                        navController.navigate("full_screen_gallery/$incidentId/$index")
                    }
                )
            }

            composable("admin_home") {
                AdminHomeScreen(navController)
            }
            composable("admin_incidents") {
                AdminIncidentScreen(navController)
            }
            composable("admin_tags") {
                AdminTagScreen()
            }
            composable("admin_users") {
                AdminUserScreen(navController)
            }

            composable("full_screen_gallery/{incidentId}/{startPosition}") { backStackEntry ->
                val incidentId = backStackEntry.arguments?.getString("incidentId") ?: return@composable
                val startPosition = backStackEntry.arguments?.getString("startPosition")?.toIntOrNull() ?: 0
                FullScreenGalleryScreen(
                    incidentId = incidentId,
                    startPosition = startPosition,
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable("add_contact") {
                AddContactScreen(
                    onContactAdded = {
                        navController.popBackStack()
                })
            }

        }
    }
}

