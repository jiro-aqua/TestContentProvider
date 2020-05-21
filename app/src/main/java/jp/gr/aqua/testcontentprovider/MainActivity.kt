package jp.gr.aqua.testcontentprovider

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.DatabaseUtils
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class MainActivity : AppCompatActivity() {

    var uri: Uri? = null
    val req_code = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val openDocument = registerForActivityResult(OpenDocument()) {
            it?.let{
                uri = it
                openUri(it)
                buttonOpen.isEnabled = false
                buttonSave.isEnabled = true
            }
        }

        intent.data?.let{
            uri = it
            openUri(it)
            buttonOpen.isEnabled = false
            buttonSave.isEnabled = true
        }?: run {
            buttonOpen.isEnabled = true
            buttonSave.isEnabled = false
        }

        buttonOpen.setOnClickListener {
            openDocument.launch(emptyArray())
        }

        buttonSave.setOnClickListener {
            if ( uri != null ) {
                Single.just(uri to editContent.text.toString())
                        .subscribeOn(Schedulers.io())
                        .doOnSuccess { saveContent(it.first!!, it.second) }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { Toast.makeText(this, "Success!", Toast.LENGTH_LONG).show(); finish() },
                                { it.printStackTrace() }
                        )
            }
        }
    }

    private fun openUri(uri:Uri)
    {
        Single.just(uri)
                .subscribeOn(Schedulers.io())
                .map { loadContent(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            textUri.text = it.first
                            title = it.second
                            editContent.setText(it.third)
                        },
                        { it.printStackTrace() }
                )
    }

    @Throws(Exception::class)
    private fun loadContent(uri: Uri): Triple<String?, String?, String?> {
        return Triple(uri.toString(),
                getTitle(uri),
                contentResolver.openInputStream(uri)?.bufferedReader(charset = Charsets.UTF_8).use { it?.readText() }
        )
    }

    @Throws(Exception::class)
    private fun saveContent(uri: Uri, string: String) {
        contentResolver.openOutputStream(uri)?.bufferedWriter(charset = Charsets.UTF_8).use { it?.write(string) }
    }

    @Throws(Exception::class)
    private fun getTitle(uri: Uri): String {
        return contentResolver.query(uri, null, null, null, null)?.use {
            it.moveToFirst()
            DatabaseUtils.dumpCursor(it)

            it.moveToFirst()
            it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }!!
    }

    private class OpenDocument : ActivityResultContract<Array<String>, Uri>() {

        @CallSuper
        override fun createIntent(context: Context, input: Array<String>): Intent {
            return Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .setType("*/*")
        }

        override fun getSynchronousResult(context: Context,
                                          input: Array<String>): ActivityResultContract.SynchronousResult<Uri>? {
            return null
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
        }
    }

}
