package com.nano.ocr.attribute

import com.nano.ocr.IdCardEntityDto
import com.nanobnk.compliance.model.attribute.ApiResponseCode


class ApiResponseDto <IdCardEntityDto>(
        var code: ApiResponseCode? = null,
        var message:  String? = null,
        var data:  com.nano.ocr.IdCardEntityDto? = null
)