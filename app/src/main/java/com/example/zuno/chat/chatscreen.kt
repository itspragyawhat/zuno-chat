package com.example.zuno.chat

import android.app.Activity
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()

    var messageText by remember { mutableStateOf("") }

    var scheduleMode by remember {
        mutableStateOf(false)
    }

    var scheduleMinutes by remember {
        mutableStateOf("")
    }

    var scheduledPreviewText by remember {
        mutableStateOf("")
    }

    var scheduledPreviewDelay by remember {
        mutableStateOf("")
    }

    var callScheduleMode by remember {
        mutableStateOf(false)
    }

    var callMinutes by remember {
        mutableStateOf("")
    }

    var scheduledCallPreview by remember {
        mutableStateOf("")
    }

    var autoReplyEnabled by remember {
        mutableStateOf(true)
    }

    var showBusinessPanel by remember {
        mutableStateOf(false)
    }

    val messages = remember {
        mutableStateListOf<Message>()
    }

    val repliedDocIds = remember {
        mutableStateListOf<String>()
    }

    val listState = rememberLazyListState()

    var recorder: MediaRecorder? = null

    var audioFile by remember {
        mutableStateOf("")
    }

    var isRecording by remember {
        mutableStateOf(false)
    }

    val quickReplies = listOf(
        "Hello 👋",
        "How can I help you?",
        "Please share details",
        "Thank you 😊"
    )



    fun sendMessage(text: String) {

        if (text.isBlank()) return

        val msg = Message(
            text = text,
            sender = "me",
            time = System.currentTimeMillis()
        )

        db.collection("chats")
            .document(userName)
            .collection("messages")
            .add(msg)
    }



    fun scheduleMessage(
        text: String,
        minutes: Int
    ) {

        val cleanText = text.trim()

        if (cleanText.isBlank()) {

            Toast.makeText(
                context,
                "Type message first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        scheduledPreviewText = cleanText

        scheduledPreviewDelay =
            "$minutes minute(s)"

        messageText = ""

        scheduleMode = false

        scheduleMinutes = ""

        Toast.makeText(
            context,
            "Message scheduled successfully",
            Toast.LENGTH_SHORT
        ).show()

        val finalMessage = cleanText

        scope.launch {

            val millis =
                minutes * 60 * 1000L

            delay(millis)

            db.collection("chats")
                .document(userName)
                .collection("messages")
                .add(
                    Message(
                        text = finalMessage,
                        sender = "me",
                        time = System.currentTimeMillis()
                    )
                )
                .addOnSuccessListener {

                    Toast.makeText(
                        context,
                        "Scheduled message sent",
                        Toast.LENGTH_SHORT
                    ).show()

                    scheduledPreviewText = ""
                    scheduledPreviewDelay = ""
                }
        }
    }



    fun scheduleCall(minutes: Int) {

        if (minutes <= 0) {

            Toast.makeText(
                context,
                "Enter call delay first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        scheduledCallPreview =
            "Call with $userName after $minutes minute(s)"

        callScheduleMode = false

        callMinutes = ""

        Toast.makeText(
            context,
            "Call scheduled after $minutes minute(s)",
            Toast.LENGTH_SHORT
        ).show()

        scope.launch {

            delay(minutes * 60_000L)

            Toast.makeText(
                context,
                "Scheduled call time with $userName 📞",
                Toast.LENGTH_LONG
            ).show()

            scheduledCallPreview = ""
        }
    }



    fun getAutoReply(text: String): String? {

        val lower = text.lowercase()

        return when {

            lower.contains("hi") ||
                    lower.contains("hello") ->
                "Hello 👋 Welcome to Zuno."

            lower.contains("price") ->
                "Pricing starts from ₹499."

            lower.contains("location") ->
                "We are located in Jaipur."

            else -> null
        }
    }



    val imagePicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                val uri = result.data?.data

                if (uri != null) {

                    val msg = Message(
                        sender = "me",
                        time = System.currentTimeMillis(),
                        imageUrl = uri.toString()
                    )

                    db.collection("chats")
                        .document(userName)
                        .collection("messages")
                        .add(msg)
                }
            }
        }



    val voiceLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                val spokenText = result.data
                    ?.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                    )
                    ?.firstOrNull()

                if (!spokenText.isNullOrBlank()) {

                    messageText = spokenText
                }
            }
        }



    LaunchedEffect(userName) {

        db.collection("chats")
            .document(userName)
            .collection("messages")
            .orderBy(
                "time",
                Query.Direction.ASCENDING
            )
            .addSnapshotListener { value, _ ->

                if (value != null) {

                    messages.clear()

                    value.documents.forEach { doc ->

                        val msg =
                            doc.toObject(Message::class.java)

                        if (msg != null) {

                            messages.add(msg)

                            if (
                                autoReplyEnabled &&
                                msg.sender != "me" &&
                                !repliedDocIds.contains(doc.id)
                            ) {

                                repliedDocIds.add(doc.id)

                                val reply =
                                    getAutoReply(msg.text)

                                if (reply != null) {

                                    val autoMsg = Message(
                                        text = reply,
                                        sender = "me",
                                        time = System.currentTimeMillis()
                                    )

                                    db.collection("chats")
                                        .document(userName)
                                        .collection("messages")
                                        .add(autoMsg)
                                }
                            }
                        }
                    }
                }
            }
    }



    LaunchedEffect(messages.size) {

        if (messages.isNotEmpty()) {

            listState.animateScrollToItem(
                messages.size - 1
            )
        }
    }



    fun startRecording() {

        try {

            val file = File(
                context.cacheDir,
                "voice_${System.currentTimeMillis()}.m4a"
            )

            audioFile = file.absolutePath

            recorder = MediaRecorder().apply {

                setAudioSource(
                    MediaRecorder.AudioSource.MIC
                )

                setOutputFormat(
                    MediaRecorder.OutputFormat.MPEG_4
                )

                setAudioEncoder(
                    MediaRecorder.AudioEncoder.AAC
                )

                setOutputFile(audioFile)

                prepare()
                start()
            }

            isRecording = true

        } catch (e: Exception) {

            Toast.makeText(
                context,
                "Recording failed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }



    fun stopRecording() {

        try {

            recorder?.apply {

                stop()
                release()
            }

            val msg = Message(
                text = "Voice Message",
                sender = "me",
                time = System.currentTimeMillis(),
                audioPath = audioFile
            )

            db.collection("chats")
                .document(userName)
                .collection("messages")
                .add(msg)

        } catch (e: Exception) {

            Toast.makeText(
                context,
                "Audio failed",
                Toast.LENGTH_SHORT
            ).show()

        } finally {

            recorder = null
            isRecording = false
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftCream)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BaseBlue)
                .padding(12.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "←",
                color = Color.White,
                fontSize = 22.sp,

                modifier = Modifier.clickable {
                    onBack()
                }
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SoftPink),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = userName.first().toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = userName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Online",
                    color = SoftCream,
                    fontSize = 12.sp
                )
            }

            Text(
                text = "💼",
                color = Color.White,
                fontSize = 20.sp,

                modifier = Modifier.clickable {
                    showBusinessPanel =
                        !showBusinessPanel
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "📞",
                color = Color.White,
                fontSize = 20.sp,

                modifier = Modifier.clickable {

                    callScheduleMode =
                        !callScheduleMode

                    scheduleMode = false

                    Toast.makeText(
                        context,
                        "Call scheduler opened",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }



        if (showBusinessPanel) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(10.dp)
            ) {

                Text(
                    text = "Business Tools",
                    color = BaseBlue,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Commands: /price /location /hi",
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = "Auto Reply",
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = autoReplyEnabled,

                        onCheckedChange = {
                            autoReplyEnabled = it
                        }
                    )
                }
            }
        }



        if (scheduledPreviewText.isNotBlank()) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(Color.White)
                    .padding(10.dp)
            ) {

                Text(
                    text = "⏰ Scheduled Message",
                    color = BaseBlue,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Message: $scheduledPreviewText",
                    fontSize = 13.sp
                )

                Text(
                    text = "Sends after: $scheduledPreviewDelay",
                    fontSize = 13.sp
                )

                Text(
                    text = "Status: Scheduled",
                    color = SoftPink,
                    fontSize = 13.sp
                )
            }
        }



        if (scheduledCallPreview.isNotBlank()) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(Color.White)
                    .padding(10.dp)
            ) {

                Text(
                    text = "📞 Scheduled Call",
                    color = BaseBlue,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = scheduledCallPreview,
                    fontSize = 13.sp
                )

                Text(
                    text = "Status: Scheduled",
                    color = SoftPink,
                    fontSize = 13.sp
                )
            }
        }



        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),

            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            items(quickReplies) { quick ->

                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(20.dp)
                        )
                        .background(SoftPink)

                        .clickable {
                            messageText = quick
                        }

                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        )
                ) {

                    Text(
                        text = quick,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }



        if (callScheduleMode) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "📞",
                    fontSize = 22.sp
                )

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = callMinutes,

                    onValueChange = {

                        callMinutes =
                            it.filter { ch ->
                                ch.isDigit()
                            }
                    },

                    modifier = Modifier.weight(1f),

                    placeholder = {
                        Text("Call after minutes")
                    },

                    singleLine = true,

                    colors = TextFieldDefaults.colors(
                        focusedContainerColor =
                            SoftCream,

                        unfocusedContainerColor =
                            SoftCream
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(20.dp)
                        )
                        .background(BaseBlue)

                        .clickable {

                            val mins =
                                callMinutes.toIntOrNull()
                                    ?: 0

                            scheduleCall(mins)
                        }

                        .padding(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        )
                ) {

                    Text(
                        text = "Set Call",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }



        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f)
        ) {

            items(messages) { msg ->

                val isMe =
                    msg.sender == "me"

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        if (isMe)
                            Arrangement.End
                        else
                            Arrangement.Start
                ) {

                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(
                                RoundedCornerShape(12.dp)
                            )

                            .background(
                                if (isMe)
                                    SoftPink
                                else
                                    Color.White
                            )

                            .padding(10.dp)
                    ) {

                        Column {

                            when {

                                msg.audioPath != null -> {

                                    Text(
                                        text = "▶ Play Voice",

                                        modifier =
                                            Modifier.clickable {

                                                try {

                                                    val player =
                                                        MediaPlayer()

                                                    player.setDataSource(
                                                        msg.audioPath
                                                    )

                                                    player.prepare()
                                                    player.start()

                                                } catch (e: Exception) {

                                                    Toast.makeText(
                                                        context,
                                                        "Audio error",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                    )
                                }

                                msg.imageUrl != null -> {

                                    AsyncImage(
                                        model = msg.imageUrl,
                                        contentDescription = null,

                                        modifier =
                                            Modifier.size(150.dp)
                                    )
                                }

                                else -> {

                                    Text(msg.text)
                                }
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text = SimpleDateFormat(
                                    "hh:mm a",
                                    Locale.getDefault()
                                ).format(Date(msg.time)),

                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }



        if (scheduleMode) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "⏰",
                    fontSize = 22.sp
                )

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = scheduleMinutes,

                    onValueChange = {

                        scheduleMinutes =
                            it.filter { ch ->
                                ch.isDigit()
                            }
                    },

                    modifier = Modifier.weight(1f),

                    placeholder = {
                        Text("Send after minutes")
                    },

                    singleLine = true,

                    colors = TextFieldDefaults.colors(
                        focusedContainerColor =
                            SoftCream,

                        unfocusedContainerColor =
                            SoftCream
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(20.dp)
                        )
                        .background(BaseBlue)

                        .clickable {

                            Toast.makeText(
                                context,
                                "Now type message and press ➤",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        .padding(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        )
                ) {

                    Text(
                        text = "Set",
                        color = Color.White
                    )
                }
            }
        }



        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SoftCream)
                .imePadding()
                .padding(8.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            TextField(
                value = messageText,

                onValueChange = {
                    messageText = it
                },

                modifier = Modifier.weight(1f),

                placeholder = {

                    Text(
                        if (scheduleMode)
                            "Type scheduled message..."
                        else
                            "Message"
                    )
                },

                colors = TextFieldDefaults.colors(
                    focusedContainerColor =
                        Color.White,

                    unfocusedContainerColor =
                        Color.White
                )
            )

            Spacer(modifier = Modifier.width(6.dp))



            Text(
                text = "📷",
                fontSize = 22.sp,

                modifier = Modifier.clickable {

                    val intent = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )

                    imagePicker.launch(intent)
                }
            )

            Spacer(modifier = Modifier.width(8.dp))



            Text(
                text = "⏰",
                fontSize = 24.sp,

                modifier = Modifier.clickable {

                    scheduleMode =
                        !scheduleMode

                    callScheduleMode = false

                    scheduleMinutes = ""

                    Toast.makeText(
                        context,

                        if (scheduleMode)
                            "Schedule mode ON"
                        else
                            "Schedule mode OFF",

                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            Spacer(modifier = Modifier.width(8.dp))



            Text(
                text =
                    if (isRecording)
                        "■"
                    else
                        "🎤",

                fontSize = 22.sp,

                modifier = Modifier.clickable {

                    if (!isRecording)
                        startRecording()
                    else
                        stopRecording()
                }
            )

            Spacer(modifier = Modifier.width(8.dp))



            Text(
                text = "🎙",
                fontSize = 22.sp,

                modifier = Modifier.clickable {

                    val intent = Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                    ).apply {

                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )

                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE,
                            Locale.getDefault()
                        )
                    }

                    voiceLauncher.launch(intent)
                }
            )

            Spacer(modifier = Modifier.width(8.dp))



            Text(
                text = "➤",
                fontSize = 24.sp,

                modifier = Modifier.clickable {

                    if (scheduleMode) {

                        val mins =
                            scheduleMinutes.toIntOrNull()
                                ?: 0

                        if (mins <= 0) {

                            Toast.makeText(
                                context,
                                "Enter minutes first",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {

                            scheduleMessage(
                                messageText,
                                mins
                            )
                        }

                    } else {

                        sendMessage(messageText)

                        messageText = ""
                    }
                }
            )



        }
    }
}