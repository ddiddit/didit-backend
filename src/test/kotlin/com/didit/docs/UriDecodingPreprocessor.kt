package com.didit.docs

import org.springframework.restdocs.operation.OperationRequest
import org.springframework.restdocs.operation.OperationRequestFactory
import org.springframework.restdocs.operation.OperationResponse
import org.springframework.restdocs.operation.preprocess.OperationPreprocessor
import org.springframework.web.util.UriUtils
import java.net.URI
import java.nio.charset.StandardCharsets

class UriDecodingPreprocessor : OperationPreprocessor {
    override fun preprocess(request: OperationRequest): OperationRequest {
        val decoded = UriUtils.decode(request.uri.toString(), StandardCharsets.UTF_8)
        return OperationRequestFactory().create(
            URI.create(decoded),
            request.method,
            request.content,
            request.headers,
            request.parts,
        )
    }

    override fun preprocess(response: OperationResponse): OperationResponse = response
}
