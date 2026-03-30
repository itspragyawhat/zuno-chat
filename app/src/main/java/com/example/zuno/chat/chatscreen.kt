package com.example.zuno.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.zuno.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class Message(
    val text: String,
    val isMe: Boolean,
    val time: String,
    val seen: Boolean
)

@Composable
fun ChatScreen() {

    var messageText by remember { mutableStateOf("") }

    val userName = "Rahul Sharma"

    val messages = remember {
        mutableStateListOf<Message>()
    }

    val listState = rememberLazyListState()

    // REAL TIME CLOCK
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat(
                "hh:mm a",
                Locale.getDefault()
            ).format(Date())

            delay(1000)
        }
    }

    // Always scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {

        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Image(
                painter = painterResource(id = R.drawable.profileapp),
                contentDescription = "Profile",
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(50))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {

                Text(
                    text = userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Text(
                    text = "Online • $currentTime",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {

            items(messages) { msg ->

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        if (msg.isMe) Arrangement.End
                        else Arrangement.Start
                ) {

                    Column(
                        horizontalAlignment =
                            if (msg.isMe) Alignment.End
                            else Alignment.Start
                    ) {

                        Box(
                            modifier = Modifier
                                .background(
                                    color =
                                        if (msg.isMe)
                                            Color(0xFFDCF8C6)
                                        else
                                            Color.LightGray,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(10.dp)
                        ) {

                            Column {

                                Text(msg.text)

                                Spacer(modifier = Modifier.height(4.dp))

                                Row {

                                    Text(
                                        msg.time,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )

                                    if (msg.isMe) {

                                        Spacer(
                                            modifier = Modifier.width(4.dp)
                                        )

                                        Text(
                                            if (msg.seen) "✓✓" else "✓",
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Message input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type message...") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {

                    if (messageText.isNotEmpty()) {

                        val time = SimpleDateFormat(
                            "hh:mm a",
                            Locale.getDefault()
                        ).format(Date())

                        messages.add(
                            Message(
                                text = messageText,
                                isMe = true,
                                time = time,
                                seen = true
                            )
                        )

                        messageText = ""
                    }
                }
            ) {
                Text("Send")
            }
        }
    }
}