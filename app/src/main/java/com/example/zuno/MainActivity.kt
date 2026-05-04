package com.example.zuno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zuno.chat.ChatListScreen
import com.example.zuno.chat.ChatScreen
import com.example.zuno.ui.theme.BaseBlue
import com.example.zuno.ui.theme.SoftCream
import com.example.zuno.ui.theme.ZunoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZunoTheme {
                var selectedUser by remember { mutableStateOf<String?>(null) }

                val users = listOf(
                    "Rahul Sharma",
                    "Aman Verma",
                    "Priya Singh"
                )

                val lockedChats = remember { mutableStateMapOf<String, Boolean>() }

                var pin by remember { mutableStateOf("1234") }
                var enteredPin by remember { mutableStateOf("") }
                var showLockScreen by remember { mutableStateOf(false) }
                var tempUser by remember { mutableStateOf<String?>(null) }

                when {
                    showLockScreen -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SoftCream)
                                .padding(20.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔒 Enter PIN", fontSize = 20.sp, color = BaseBlue)

                            Spacer(modifier = Modifier.height(12.dp))

                            TextField(
                                value = enteredPin,
                                onValueChange = { enteredPin = it },
                                placeholder = { Text("Enter PIN") }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (enteredPin == pin) {
                                        selectedUser = tempUser
                                        showLockScreen = false
                                        enteredPin = ""
                                    }
                                }
                            ) {
                                Text("Unlock")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    showLockScreen = false
                                    enteredPin = ""
                                    tempUser = null
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    }

                    selectedUser == null -> {
                        ChatListScreen(
                            users = users,
                            lockedChats = lockedChats,
                            onChatClick = { userName ->
                                if (lockedChats[userName] == true) {
                                    tempUser = userName
                                    showLockScreen = true
                                } else {
                                    selectedUser = userName
                                }
                            },
                            onLockToggle = { user ->
                                lockedChats[user] = !(lockedChats[user] ?: false)
                            }
                        )
                    }

                    else -> {
                        ChatScreen(
                            userName = selectedUser!!,
                            onBack = { selectedUser = null }
                        )
                    }
                }
            }
        }
    }
}