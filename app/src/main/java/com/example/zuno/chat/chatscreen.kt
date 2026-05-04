package com.example.zuno.chat

import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.zuno.ui.theme.BaseBlue
import com.example.zuno.ui.theme.SoftCream
import com.example.zuno.ui.theme.SoftPink
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Message(
    val text: String = "",
    val sender: String = "",
    val time: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val audioPath: String? = null
)

@Composable
fun ChatScreen(
    userName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Message>() }
    val listState = rememberLazyListState()

    var recorder: MediaRecorder? = null
    var audioFile by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }

    fun handleVoiceCommand(spokenTextRaw: String) {
        val spokenText = spokenTextRaw.lowercase().trim()

        if (spokenText.isBlank()) return

        if (spokenText.contains("hey zuno") || spokenText.contains("ok zuno")) {
            val command = spokenText
                .replace("hey zuno", "")
                .replace("ok zuno", "")
                .trim()

            when {
                command.startsWith("send") -> {
                    val msgText = command.removePrefix("send").trim()
                    if (msgText.isNotEmpty()) {
                        val msg = Message(
                            text = msgText,
                            sender = "me",
                            time = System.currentTimeMillis()
                        )
                        db.collection("chats")
                            .document(userName)
                            .collection("messages")
                            .add(msg)
                    }
                }

                command.contains("clear") -> {
                    messageText = ""
                }

                command.contains("call") -> {
                    Toast.makeText(context, "Calling coming soon", Toast.LENGTH_SHORT).show()
                }

                else -> {
                    messageText = command
                }
            }
        } else {
            messageText = spokenTextRaw
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val spokenText = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()

                if (!spokenText.isNullOrBlank()) {
                    handleVoiceCommand(spokenText)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Voice input failed", Toast.LENGTH_SHORT).show()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    isUploadingImage = true
                    val msg = Message(
                        sender = "me",
                        time = System.currentTimeMillis(),
                        imageUrl = uri.toString()
                    )

                    db.collection("chats")
                        .document(userName)
                        .collection("messages")
                        .add(msg)

                    isUploadingImage = false
                }
            }
        } catch (e: Exception) {
            isUploadingImage = false
            Toast.makeText(context, "Could not pick image", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(userName) {
        db.collection("chats")
            .document(userName)
            .collection("messages")
            .orderBy("time", Query.Direction.ASCENDING)
            .addSnapshotListener { value, _ ->
                if (value != null) {
                    messages.clear()
                    value.documents.forEach {
                        it.toObject(Message::class.java)?.let(messages::add)
                    }
                }
            }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun startRecording() {
        try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            audioFile = file.absolutePath

            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile)
                prepare()
                start()
            }

            isRecording = true
        } catch (e: Exception) {
            Toast.makeText(context, "Could not start recording", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }

            val msg = Message(
                text = "Voice message",
                sender = "me",
                time = System.currentTimeMillis(),
                audioPath = audioFile
            )

            db.collection("chats")
                .document(userName)
                .collection("messages")
                .add(msg)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not save voice message", Toast.LENGTH_SHORT).show()
        } finally {
            recorder = null
            isRecording = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftCream)
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BaseBlue)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "←",
                color = Color.White,
                fontSize = 22.sp,
                modifier = Modifier.clickable { onBack() }
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BaseBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(userName.first().toString(), color = SoftCream)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(userName, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Online", color = SoftCream, fontSize = 12.sp)
            }

            Text("📞", color = Color.White)
            Spacer(modifier = Modifier.width(10.dp))
            Text("📹", color = Color.White)
        }

        val quickReplies = listOf("Hi 👋", "On my way", "Busy", "Call later", "Send details")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickReplies.forEach { quick ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SoftPink)
                        .clickable { messageText = quick }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(quick, color = Color.White, fontSize = 12.sp)
                }
            }
        }

        if (isUploadingImage) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uploading image...", color = BaseBlue, fontSize = 12.sp)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f)
        ) {
            items(messages) { msg ->
                val isMe = msg.sender == "me"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isMe) SoftPink else Color.White)
                            .padding(10.dp)
                    ) {
                        Column {
                            when {
                                msg.audioPath != null -> {
                                    Text(
                                        "▶ Play Voice",
                                        color = BaseBlue,
                                        modifier = Modifier.clickable {
                                            try {
                                                val player = MediaPlayer()
                                                player.setDataSource(msg.audioPath)
                                                player.prepare()
                                                player.start()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error playing audio", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }

                                msg.imageUrl != null -> {
                                    AsyncImage(
                                        model = msg.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(150.dp)
                                    )
                                }

                                else -> {
                                    Text(msg.text)
                                }
                            }

                            Text(
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.time)),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SoftCream)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                "📷",
                modifier = Modifier.clickable {
                    val intent = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                    imagePicker.launch(intent)
                }
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                if (isRecording) "■" else "🎤",
                modifier = Modifier.clickable {
                    if (!isRecording) startRecording() else stopRecording()
                }
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                "🎙",
                modifier = Modifier.clickable {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say: Hey Zuno send hello")
                        }
                        voiceLauncher.launch(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Voice assistant unavailable", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                "➤",
                modifier = Modifier.clickable {
                    if (messageText.isNotBlank()) {
                        val msg = Message(
                            text = messageText,
                            sender = "me",
                            time = System.currentTimeMillis()
                        )

                        db.collection("chats")
                            .document(userName)
                            .collection("messages")
                            .add(msg)

                        messageText = ""
                    }
                }
            )
        }
    }
}