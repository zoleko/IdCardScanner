package com.nanobnk.compliance.model.attribute

enum class ApiResponseCode {
    S, F, E;

    fun getValue() : String {
        return this.name
    }
}