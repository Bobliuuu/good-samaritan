package com.example.myapplication.admin

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class FakeAdminTagViewModel : ViewModel() {
    private val _tags = MutableStateFlow<List<String>>(listOf("Tag1", "Tag2"))
    val tags: StateFlow<List<String>> = _tags

    fun addTag(tag: String) {
        _tags.value = _tags.value + tag
    }

    fun deleteTag(tag: String) {
        _tags.value = _tags.value.filter { it != tag }
    }
}

@Composable
fun AdminTagScreen(viewModel: FakeAdminTagViewModel = viewModel()) {
    var newTag by remember { mutableStateOf("") }
    val tags by viewModel.tags.collectAsState()

    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Manage Tags", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = newTag,
            onValueChange = { newTag = it },
            label = { Text("New Tag") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (newTag.isNotBlank()) {
                    viewModel.addTag(newTag)
                    newTag = ""
                }
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Add Tag")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Current Tags:")

        tags.forEach { tag ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("• $tag")
                Button(onClick = { viewModel.deleteTag(tag) }) {
                    Text("Delete")
                }
            }
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class AdminTagScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun adminTagScreenDisplaysAllElements() {
        // Set the content to AdminTagScreen with a FakeAdminTagViewModel.
        composeTestRule.setContent {
            // Provide our FakeAdminTagViewModel.
            AdminTagScreen(viewModel = FakeAdminTagViewModel())
        }

        // Verify the header text.
        composeTestRule.onNodeWithText("Manage Tags").assertExists()

        // Verify that the OutlinedTextField label exists.
        composeTestRule.onNodeWithText("New Tag").assertExists()

        // Verify that the Add Tag button exists.
        composeTestRule.onNodeWithText("Add Tag").assertExists()

        // Verify that the "Current Tags:" label exists.
        composeTestRule.onNodeWithText("Current Tags:").assertExists()

        // Verify that the two tags are displayed.
        composeTestRule.onNodeWithText("• Tag1").assertExists()
        composeTestRule.onNodeWithText("• Tag2").assertExists()

        // Verify that each tag row has a Delete button.
        // Expect two Delete buttons for two tags.
        composeTestRule.onAllNodesWithText("Delete").assertCountEquals(2)
    }
}
