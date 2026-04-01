package com.didit.docs

import org.springframework.restdocs.operation.preprocess.OperationRequestPreprocessor
import org.springframework.restdocs.operation.preprocess.OperationResponsePreprocessor
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint

object ApiDocumentUtils {
    fun getDocumentRequest(): OperationRequestPreprocessor =
        preprocessRequest(
            UriDecodingPreprocessor(),
            prettyPrint(),
        )

    fun getDocumentResponse(): OperationResponsePreprocessor = preprocessResponse(prettyPrint())
}
