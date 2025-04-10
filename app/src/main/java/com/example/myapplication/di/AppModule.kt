package com.example.myapplication.di

import com.example.myapplication.admin.adminhome.AdminHomeViewModel
import com.example.myapplication.admin.adminincident.AdminIncidentViewModel
import com.example.myapplication.admin.admintag.AdminTagViewModel
import com.example.myapplication.admin.adminuser.AdminUserViewModel
import com.example.myapplication.ui.components.BottomNavViewModel
import com.example.myapplication.screens.empty.EmptyViewModel
import com.example.myapplication.screens.map.MapViewModel
import com.example.myapplication.screens.addincident.AddIncidentViewModel
import com.example.myapplication.screens.notification.NotificationViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.example.myapplication.screens.incidentdetail.IncidentDetailViewModel
import com.example.myapplication.navigation.ContactsViewModel
import com.example.myapplication.screens.addcontact.AddContactViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage

val appModule = module {
    viewModel { BottomNavViewModel() }
    viewModel { EmptyViewModel() }
    viewModel { MapViewModel() }
    viewModel { (incidentId: String) -> IncidentDetailViewModel(get(), incidentId) }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseAuth.getInstance() }
    single { FirebaseStorage.getInstance() }
    viewModel { ContactsViewModel(get(), get()) }
    viewModel { AddContactViewModel(get(), get()) }
    viewModel { AddIncidentViewModel(get(), get(), get()) }
    viewModel { NotificationViewModel(get(), get()) }
    viewModel { AddContactViewModel(get(), get()) }
    viewModel { AdminHomeViewModel() }
    viewModel { AdminIncidentViewModel(get()) }
    viewModel { AdminTagViewModel(get()) }
    viewModel { AdminUserViewModel() }
}
