package jp.gr.aqua.testcontentprovider

import android.database.DatabaseUtils
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val uri = intent.data
        if (uri != null) {
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

            buttonSave.setOnClickListener {
                Single.just(uri to editContent.text.toString())
                        .subscribeOn(Schedulers.io())
                        .doOnSuccess { saveContent( it.first , it.second ) }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { Toast.makeText(this, "Success!", Toast.LENGTH_LONG).show(); finish() },
                                { it.printStackTrace() }
                        )
            }
        }else{
            Toast.makeText(this, "Open a text file from Google Drive App!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    @Throws(Exception::class)
    private fun loadContent(uri: Uri): Triple<String, String,String> {
        return Triple( uri.toString() ,
                getTitle(uri) ,
                contentResolver.openInputStream(uri).bufferedReader(charset = Charsets.UTF_8).use { it.readText() }
                )
    }

    @Throws(Exception::class)
    private fun saveContent(uri: Uri, string: String) {
        contentResolver.openOutputStream(uri).bufferedWriter(charset = Charsets.UTF_8).use{ it.write(string) }
    }

    @Throws(Exception::class)
    private fun getTitle(uri : Uri): String {
        return contentResolver.query(uri, null, null, null, null)?.use {
            it.moveToFirst()
            DatabaseUtils.dumpCursor(it)

            it.moveToFirst()
            it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }!!
    }
}
