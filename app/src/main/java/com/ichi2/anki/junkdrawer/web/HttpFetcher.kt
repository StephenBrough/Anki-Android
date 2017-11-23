/****************************************************************************************
 * Copyright (c) 2013 Bibek Shrestha <bibekshrestha></bibekshrestha>@gmail.com>                          *
 * Copyright (c) 2013 Zaur Molotnikov <qutorial></qutorial>@gmail.com>                              *
 * Copyright (c) 2013 Nicolas Raoul <nicolas.raoul></nicolas.raoul>@gmail.com>                           *
 * Copyright (c) 2013 Flavio Lerda <flerda></flerda>@gmail.com>                                   *
 * *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 * *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 * *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.                           *
 */

package com.ichi2.anki.junkdrawer.web

import android.content.Context

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.HttpConnectionParams
import org.apache.http.protocol.BasicHttpContext

import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

/**
 * Helper class to donwload from web.
 *
 *
 * Used in AsyncTasks in Translation and Pronunication activities, and more...
 */
object HttpFetcher {


    @JvmOverloads
    fun fetchThroughHttp(address: String, encoding: String = "utf-8"): String {

        try {
            val httpClient = DefaultHttpClient()
            val params = httpClient.params
            HttpConnectionParams.setConnectionTimeout(params, 10000)
            HttpConnectionParams.setSoTimeout(params, 60000)
            val localContext = BasicHttpContext()
            val httpGet = HttpGet(address)
            val response = httpClient.execute(httpGet, localContext)
            if (response.statusLine.statusCode != 200) {
                return "FAILED"
            }

            val reader = BufferedReader(InputStreamReader(response.entity.content,
                    Charset.forName(encoding)))

            val stringBuilder = StringBuilder()

            var line: String = reader.readLine()
            while (line != null) {
                stringBuilder.append(line)
                line = reader.readLine()
            }

            return stringBuilder.toString()

        } catch (e: Exception) {
            return "FAILED with exception: " + e.message
        }

    }

    fun downloadFileToSdCard(UrlToFile: String, context: Context, prefix: String): String {
        var str = downloadFileToSdCardMethod(UrlToFile, context, prefix, "GET")
        if (str.startsWith("FAIL")) {
            str = downloadFileToSdCardMethod(UrlToFile, context, prefix, "POST")
        }

        return str
    }


    private fun downloadFileToSdCardMethod(UrlToFile: String, context: Context, prefix: String, method: String): String {
        try {
            val url = URL(UrlToFile)

            val extension = UrlToFile.substring(UrlToFile.length - 4)

            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = method
            urlConnection.setRequestProperty("Referer", "com.ichi2.anki")
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ")
            urlConnection.setRequestProperty("Accept", "*/*")
            urlConnection.connectTimeout = 10000
            urlConnection.readTimeout = 60000
            urlConnection.connect()

            val file = File.createTempFile(prefix, extension, context.cacheDir)

            val fileOutput = FileOutputStream(file)
            val inputStream = urlConnection.inputStream

            val buffer = ByteArray(1024)
            var bufferLength = inputStream.read(buffer)

            while (bufferLength > 0) {
                fileOutput.write(buffer, 0, bufferLength)
                bufferLength = inputStream.read(buffer)
            }
            fileOutput.close()

            return file.absolutePath

        } catch (e: Exception) {
            return "FAILED " + e.message
        }

    }

}
