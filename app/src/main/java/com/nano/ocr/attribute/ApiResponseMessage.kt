package com.nanobnk.compliance.model.attribute

enum class ApiResponseMessage {
    SUCCESS, FAIL, ERROR, AUTH_FAIL;

    fun getValue() : String {
        return this.name
    }
}