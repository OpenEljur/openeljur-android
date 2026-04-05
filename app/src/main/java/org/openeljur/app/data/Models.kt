package org.openeljur.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// MARK: - Envelope
@Serializable
data class Envelope<T>(
    val ok: Boolean,
    val data: T? = null,
    val error: ApiError? = null
)

@Serializable
data class ApiError(val code: String, val message: String)

// MARK: - Auth
@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val school_id: String? = null
)

@Serializable
data class LoginData(val token: String? = null)

// MARK: - Diary
@Serializable
data class DiaryRequest(
    val access_token: String,
    val school_id: String? = null,
    val student_id: String? = null,
    val days: String? = null,
    val rings: Boolean? = true
)

@Serializable
data class DiaryData(val students: Map<String, DiaryStudent>? = null)

@Serializable
data class DiaryStudent(val days: Map<String, DiaryDay>? = null)

@Serializable
data class DiaryDay(
    val alert: String? = null,
    val holiday_name: String? = null,
    val items: Map<String, DiaryItem>? = null
)

@Serializable
data class DiaryItem(
    val name: String? = null,
    val starttime: String? = null,
    val endtime: String? = null,
    val topic: String? = null,
    val homework: Map<String, Homework>? = null,
    val assessments: List<Mark>? = null,
    val files: List<FileItem>? = null,
    val resources: List<FileItem>? = null
)

@Serializable
data class Homework(val value: String? = null)

@Serializable
data class Mark(
    val value: String? = null,
    val date: String? = null,
    val comment: String? = null,
    val lesson_comment: String? = null,
    val control_type: String? = null
)

@Serializable
data class FileItem(
    val filename: String? = null,
    val link: String? = null
)

// MARK: - Marks
@Serializable
data class MarksRequest(
    val access_token: String,
    val school_id: String? = null,
    val student_id: String? = null,
    val days: String? = null
)

@Serializable
data class MarksData(val students: Map<String, StudentMarks>? = null)

@Serializable
data class StudentMarks(
    val name: String? = null,
    val title: String? = null,
    val lessons: List<LessonMarks>? = null
)

@Serializable
data class LessonMarks(
    val name: String? = null,
    val marks: List<Mark>? = null,
    val average: JsonElement? = null,
    val averageConvert: Int? = null
)

// MARK: - Messages
@Serializable
data class MessagesRequest(
    val access_token: String,
    val school_id: String? = null,
    val folder: String? = "inbox",
    val unread_only: String? = "no",
    val limit: Int? = 20,
    val page: Int? = 0
)

@Serializable
data class MessagesData(val messages: List<Message>? = null)

@Serializable
data class Message(
    val id: String? = null,
    val subject: String? = null,
    val text: String? = null,
    val short_text: String? = null,
    val date: String? = null,
    val unread: Boolean? = null,
    val user_from: MessageUser? = null,
    val users_to: List<MessageUser>? = null,
    val files: List<FileItem>? = null,
    val resources: List<FileItem>? = null
)

@Serializable
data class MessageUser(
    val name: String? = null,
    val search: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val middlename: String? = null
)

@Serializable
data class MessageInfoRequest(
    val access_token: String,
    val school_id: String? = null,
    val message_id: String
)

@Serializable
data class MessageInfoData(val message: Message? = null)

@Serializable
data class MessageSendRequest(
    val access_token: String,
    val school_id: String? = null,
    val subject: String,
    val text: String,
    val users_to: String
)

@Serializable
data class MessageSendData(val id: String? = null)

@Serializable
data class ReceiversRequest(
    val access_token: String,
    val school_id: String? = null
)

@Serializable
data class ReceiversData(val groups: List<ReceiverGroup>? = null)

@Serializable
data class ReceiverGroup(
    val key: String? = null,
    val name: String? = null,
    val users: List<ReceiverUser>? = null,
    val subgroups: List<ReceiverGroup>? = null
)

@Serializable
data class ReceiverUser(
    val name: String? = null,
    val search: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val info: String? = null
)
