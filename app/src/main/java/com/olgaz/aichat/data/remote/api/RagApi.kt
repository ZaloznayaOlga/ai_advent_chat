package com.olgaz.aichat.data.remote.api

import com.olgaz.aichat.data.remote.dto.AddDocumentRequestDto
import com.olgaz.aichat.data.remote.dto.AddDocumentResponseDto
import com.olgaz.aichat.data.remote.dto.DeleteResponseDto
import com.olgaz.aichat.data.remote.dto.DocumentListResponseDto
import com.olgaz.aichat.data.remote.dto.HealthResponseDto
import com.olgaz.aichat.data.remote.dto.SearchRequestDto
import com.olgaz.aichat.data.remote.dto.SearchResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RagApi {

    @POST("api/documents")
    suspend fun uploadDocument(@Body request: AddDocumentRequestDto): AddDocumentResponseDto

    @GET("api/documents")
    suspend fun getDocuments(): DocumentListResponseDto

    @DELETE("api/documents/{name}")
    suspend fun deleteDocument(@Path("name") name: String): DeleteResponseDto

    @POST("api/search")
    suspend fun search(@Body request: SearchRequestDto): SearchResponseDto

    @GET("api/health")
    suspend fun getHealth(): HealthResponseDto
}
