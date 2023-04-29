package com.nano.ocr;




public class Utils {



    public static String Base64UrlEncode(String arg)
    {
        String s = arg; // Regular base64 encoder
        s = s.split("=")[0]; // Remove any trailing '='s
        s = s.replace('+', '-'); // 62nd char of encoding
        s = s.replace('/', '_'); // 63rd char of encoding
        return s;
    }

    public static String Base64UrlDecode(String arg) throws Exception {
        String s = arg;
        s = s.replace('-', '+'); // 62nd char of encoding
        s = s.replace('_', '/'); // 63rd char of encoding
        switch (s.length() % 4) // Pad with trailing '='s
        {
            case 0: break; // No pad chars in this case
            case 2: s += "=="; break; // Two pad chars
            case 3: s += "="; break; // One pad char
            default: throw new Exception(
                    "Illegal base64url string!");
        }
        return s; // Standard base64 decoder
    }


}
