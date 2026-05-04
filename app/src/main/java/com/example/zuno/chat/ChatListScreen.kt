package com.example.zuno.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zuno.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatListScreen(
    users: List<String>,
    lockedChats: MutableMap<String, Boolean>,
    onChatClick: (String) -> Unit,
    onLockToggle: (String) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    val lastMessages = remember { mutableStateMapOf<String, Pair<String, String>>() }

    LaunchedEffect(users) {
        users.forEach { user ->
            try {
                val result = db.collection("chats")
                    .document(user)
                    .collection("messages")
                    .orderBy("time")
                    .limitToLast(1)
                    .get()
                    .await()

                if (!result.isEmpty) {
                    val doc = result.documents.first()

                    val text = doc.getString("text") ?: ""
                    val image = doc.getString("imageUrl")
                    val audio = doc.getString("audioPath")
                    val timeMillis = doc.getLong("time") ?: 0L

                    val preview = when {
                        image != null -> "🖼 Photo"
                        audio != null -> "🎤 Voice"
                        text.isNotBlank() -> text
                        else -> "Message"
                    }

                    val time = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        .format(Date(timeMillis))

                    lastMessages[user] = Pair(preview, time)
                } else {
                    lastMessages[user] = Pair("Tap to open chat", "")
                }
            } catch (e: Exception) {
                lastMessages[user] = Pair("Tap to open chat", "")
            }
        }
    }

    val filteredUsers = users.filter { user ->
        val preview = lastMessages[user]?.first ?: ""
        user.contains(searchText, true) || preview.contains(searchText, true)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(SoftCream)
    ) {

        // 🔵 HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BaseBlue)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Zuno", color = WhiteText, fontSize = 22.sp, fontWeight = FontWeight.Bold)

            Row {
                Text("📷", color = WhiteText, modifier = Modifier.clickable {
                    Toast.makeText(context, "Camera coming soon", Toast.LENGTH_SHORT).show()
                })
                Spacer(modifier = Modifier.width(12.dp))
                Text("⋮", color = WhiteText)
            }
        }

        // 🔍 SEARCH
        TextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp)),
            placeholder = { Text("Search chats") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = WhiteText,
                unfocusedContainerColor = WhiteText,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )

        // 📄 LIST
        LazyColumn {
            items(filteredUsers) { user ->
                val preview = lastMessages[user]?.first ?: ""
                val time = lastMessages[user]?.second ?: ""
                val isLocked = lockedChats[user] == true

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WhiteText)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Row(
                        modifier = Modifier.weight(1f).clickable { onChatClick(user) }
                    ) {
                        Box(
                            modifier = Modifier.size(50.dp)
                                .clip(CircleShape)
                                .background(BaseBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(user.first().toString(), color = SoftCream)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(user, fontWeight = FontWeight.Bold)
                            Text(preview, color = HintGray, fontSize = 12.sp)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(time, fontSize = 12.sp, color = HintGray)

                        Text(
                            if (isLocked) "🔒" else "🔓",
                            color = BaseBlue,
                            modifier = Modifier.clickable { onLockToggle(user) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(start = 70.dp))
            }
        }
    }
}