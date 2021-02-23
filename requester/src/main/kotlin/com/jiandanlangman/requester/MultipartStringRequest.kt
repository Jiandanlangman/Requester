package com.jiandanlangman.requester

import com.android.volley.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.*

internal class MultipartStringRequest(private val charset: String, enableGZIP: Boolean, url: String, headers: Map<String, String>, params: Map<String, String>, private val files: List<FileParam>, listener: Response.Listener<String>?, errorListener: Response.ErrorListener) : StringRequest(charset, 1, enableGZIP, url, headers, params, null, listener, errorListener) {

    companion object {
        private const val PREFIX = "--"
        private const val LINE_END = "\r\n"
    }

    private val boundary = UUID.randomUUID().toString()

    override fun getBodyContentType() = "multipart/form-data; boundary=$boundary"

    override fun getBody(): ByteArray {
        val baos = ByteArrayOutputStream()
        val sb = StringBuilder()
        params.forEach {
            sb.append(PREFIX).append(boundary).append(LINE_END).append("Content-Disposition: form-data; name=\"")
                .append(it.key).append("\"").append(LINE_END).append("Content-Type: text/plain; charset=")
                .append(charset).append(LINE_END).append("Content-Transfer-Encoding: 8bit").append(LINE_END)
                .append(LINE_END).append(it.value).append(LINE_END)
        }
        baos.write(sb.toString().toByteArray())
        files.forEach {
            val file = File(it.path)
            if (file.exists() && file.isFile) {
                val sb1 = StringBuilder()
                sb1.append(PREFIX).append(boundary).append(LINE_END).append("Content-Disposition: form-data; name=\"")
                    .append(it.name).append("\"").append("; fileName=\"").append(it.path).append("\"")
                    .append(LINE_END).append("Content-Type: application/octet-stream; charset=").append(charset)
                    .append(LINE_END).append(LINE_END)
                baos.write(sb1.toString().toByteArray())
                val fis = FileInputStream(file)
                val buffer = ByteArray(8196)
                var readLength: Int
                while (true) {
                    readLength = fis.read(buffer)
                    if (readLength == -1)
                        break
                    baos.write(buffer, 0, readLength)
                }
                fis.close()
                baos.write(LINE_END.toByteArray())
            }
        }
        baos.write((PREFIX + boundary + PREFIX + LINE_END).toByteArray())
        baos.flush()
        val body = baos.toByteArray()
        baos.close()
        return body
    }

    data class FileParam(val name: String, val path: String)
}