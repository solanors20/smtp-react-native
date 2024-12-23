package com.smtpreactnative

import android.content.ContentValues.TAG
import android.content.Context
import android.os.AsyncTask
import android.util.Base64
import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.facebook.react.bridge.ReadableMap
import javax.mail.AuthenticationFailedException
import javax.mail.MessagingException

class SmtpReactNativeModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return NAME
  }

  fun convertBase64ToFile(base64: String): File {
    Log.i(TAG, "start conver");
    val imgBytesData: ByteArray
    try {
      imgBytesData = Base64.decode(
        base64,
        Base64.DEFAULT
      )
    } catch (e: Exception) {
      Log.i(TAG, "FILENOTFOUND" + e.message);
      e.printStackTrace()
      throw e
    }


    val file = File.createTempFile("smtptempfile", null, reactApplicationContext.cacheDir)
    Log.i(TAG, "PATH=" + file.path);
    val filePath = file.path
    val fileOutputStream: FileOutputStream
    try {
      fileOutputStream = FileOutputStream(file)
    } catch (e: FileNotFoundException) {
      Log.i(TAG, "FILENOTFOUND");
      throw e
    }

    val bufferedOutputStream = BufferedOutputStream(
      fileOutputStream
    )
    try {
      bufferedOutputStream.write(imgBytesData)
    } catch (e: IOException) {
      Log.i(TAG, "EXCEPTION " + e.message);
      e.printStackTrace()
    } finally {
      try {
        bufferedOutputStream.close()
      } catch (e: IOException) {
        Log.i(TAG, "EXCEPTION CLOSING" + e.message);
        e.printStackTrace()
        throw e
      }
    }

    val cacheFile = File(filePath)
    Log.i(TAG, "PATH=" + cacheFile.totalSpace);
    return cacheFile
  }
  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  @ReactMethod
  fun sendEmail(readableMap: ReadableMap, promise: Promise) {


    // register activity with the connectivity manager service
    val connectivityManager = this.reactApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // if the android version is equal to M
    // or greater we need to use the
    // NetworkCapabilities to check what type of
    // network has the internet connection
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

      // Returns a Network object corresponding to
      // the currently active default data network.
      val network = connectivityManager.activeNetwork
      if(network != null) {
        Log.i(TAG, "NETWORK");
        // Representation of the capabilities of an active network.
        val activeNetwork = connectivityManager.getNetworkCapabilities(network)

        if(activeNetwork != null) {
          Log.i(TAG, "ACTIVE NEWORK");
        } else {
          throw Exception("NO ACTIVE NETWORK")
        }
      } else {
        throw Exception("NO NETWORK DETECTED")
      }

    }
    val mailhost: String = readableMap.getString("mailhost") ?: "smtp.gmail.com"
    val port: String = readableMap.getString("port") ?: "465"
    val username: String = readableMap.getString("username") ?: ""
    val password: String = readableMap.getString("password") ?: ""
    val recipientsString = readableMap.getString("recipients") ?: ""
    val recipients: Array<String> = if(readableMap.getString("recipients") !== null) recipientsString.split(",").toTypedArray<String>() else arrayOf("")
    val subject: String = readableMap.getString("subject") ?: ""
    val body: String = readableMap.getString("htmlBody") ?: ""

    val fromName: String = readableMap.getString("fromName") ?: username
    val replyToAddress: String = readableMap.getString("replyToAddress") ?: username
    val bcc: ReadableArray? = null
    val ssl: Boolean = readableMap.getBoolean("ssl") ?: true
    val attachmentsInBase64: ReadableArray? = readableMap.getArray("attachmentsInBase64")
    val attachmentsNames: ReadableArray? = readableMap.getArray("attachmentsNames")

    //val obj: ReadableMap = ReadableMap()
    AsyncTask.execute(object : Runnable {


      override fun run() {
        val m = Mail(username, password);
        m._host = mailhost
        m._port = port
        m._from = username
        m.body = body
        m._to = recipients
        m._subject = subject
        m._senderAlias = fromName
        if(attachmentsInBase64 != null && attachmentsNames != null){
          var i = 0
          while(i < attachmentsInBase64.size()) {
            m.addAttachment(attachmentsNames.getString(i), convertBase64ToFile(attachmentsInBase64.getString(i)))
            i++
          }
        }
        Log.i(TAG, "Preparing email.");
        try {
          if (m.send()) {
            Log.i(TAG, "Email sent.");
          } else {
            Log.e(TAG, "Email failed to send.");
          }
        } catch (e: AuthenticationFailedException) {
          Log.i(TAG, "AuthenticationFailedException sent.");
          e.printStackTrace();
          throw e
        } catch (e: MessagingException) {
          Log.i(TAG, "MessagingException sent.");
          e.printStackTrace();
          throw e
        } catch (e: Exception) {
          Log.i(TAG, "Exception sent.");
          e.printStackTrace();
          throw e
        }
      }
    })

    promise.resolve(1.0)
  }

  companion object {
    const val NAME = "SmtpReactNative"
  }
}
