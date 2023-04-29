package com.nano.ocr;


import java.time.LocalDateTime
import java.util.*

open class BaseAuditDto(

        var createdDate: LocalDateTime = LocalDateTime.now(),
        var createdBy: String = "SYSTEM",
        var lastModifiedDate: LocalDateTime = LocalDateTime.now(),
        var lastModifiedBy: String= "SYSTEM",

)