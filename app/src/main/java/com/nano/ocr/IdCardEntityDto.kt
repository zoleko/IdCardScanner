package com.nano.ocr;


data class IdCardEntityDto (
        var id: Long? = null,
        var idNumber: String? = "",
        var firstName: String? = "",
        var lastName: String? = "",
        var middleName: String? = "",
        var gender: String? = "",
        var occupation: String? = "",
        var dob: String? = "", //format YYYY-mm-dd, e.g. 2001-09-25
        var pob: String? = "",
        var height: String? = "",
        var country: String? = "", // Alpha-3 country code (e.g. DEU or RUS)
        var address: String? = "",
        var signature: String? = "",
        var photo: String?="",
        var idcardphoto: String?= ""

        ): BaseAuditDto()

