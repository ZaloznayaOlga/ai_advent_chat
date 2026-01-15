package com.olgaz.aichat.data.remote.api

import com.olgaz.aichat.data.remote.dto.ChatRequestDto
import com.olgaz.aichat.data.remote.dto.ChatResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatApi {

    @POST("chat/completions")
    suspend fun sendMessage(@Body request: ChatRequestDto): ChatResponseDto
}
