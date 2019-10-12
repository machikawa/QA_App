package machikawa.hidemasa.techacademy.qa_app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_question_send.*
import java.io.ByteArrayOutputStream
import android.Manifest
import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.Matrix

class QuestionSendActivity : AppCompatActivity(), View.OnClickListener, DatabaseReference.CompletionListener {

    // のっけから何かね。
    companion object {
        private val PERMISSIONS_REQUEST_CODE = 100
        private val CHOOSER_REQUEST_CODE = 100
    }
    // メニュー選んだやつと、URI.
    private var mGenre: Int = 0
    private var mPictureUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_send)

        // MainActivityのインテントからジャンルを取得。
        val extras = intent.extras
        mGenre = extras.getInt("genre")

        // 当該画面におけるタイトルとリスナーの設定
        title = "質問作成"
        sendButton.setOnClickListener(this)
        imageView.setOnClickListener(this)
    }

    // ImageView と Button 押下時に処理するのは何か
    override fun onClick(v: View?) {
        // イメージビューをくりっくしたとき. 外部ストレージへの権限確認と Show chooser
        if (v == imageView) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    showChooser()
                } else {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSIONS_REQUEST_CODE)
                    return
                }
                showChooser()
            }

            // 投稿ボタンをクリックした場合の処理
        } else if (v == sendButton) {
            // いつものキーボード引っ込め
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v!!.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)

            // Genre は root-> 子の回想 -> 孫の回想 と指定する。
            val databaseReference = FirebaseDatabase.getInstance().reference
            val genreRef = databaseReference.child(ContentsPATH).child(mGenre.toString())

            // ぼちぼち firebas　に入れる準備するで〜。
            val data = HashMap<String, String>()

            // 現在のUserのUIDを指定
            data["uid"] =  FirebaseAuth.getInstance().currentUser!!.uid

            // タイトルと本文は画面からゲット
            val title = titleText.text.toString()
            val body = bodyText.text.toString()

            // 入力チェック1) タイトルがからの時
            if (title.isEmpty()) {
                Snackbar.make(v!!,"タイトルを入力してください",Snackbar.LENGTH_LONG).show()
                return
            }
            // 入力チェック2) 質問がからの時
            if (body.isEmpty()) {
                Snackbar.make(v!!,"質問を入力してください",Snackbar.LENGTH_LONG).show()
                return
            }

            // Shared Preference から名前を取得
            val sp = PreferenceManager.getDefaultSharedPreferences(this)
            val name = sp.getString(NameKEY,"")

            data["title"] = title
            data["body"] = body
            data["name"] = name

            // Firebase に保存するために Base 64にエンコする。
            var drawable = imageView.drawable as? BitmapDrawable

            if (drawable != null) {
                val bitmap = drawable.bitmap
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val bitmapString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

                data["image"] = bitmapString
            }
            // Firebase への保存と、プログレスバーのはめ込み。
            // ??? try catch とかはいらないのかな？ OnComplete でその辺をハンドルしているようにも見える。
            genreRef.push().setValue(data, this)
            progressBar.visibility = View.VISIBLE
         }
    }

    // 権限のリクエスト
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // ユーザーが許可したばあいは　showChooser へ
                    showChooser()
                }
                return
            }
        }
    }

    // ???? 全体的にちょっと謎
    private fun showChooser() {
        // ギャラリーから選択するIntent
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)

        // カメラで撮影するIntent
        val filename = System.currentTimeMillis().toString() + ".jpg"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        mPictureUri = contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri)

        // ギャラリー選択のIntentを与えてcreateChooserメソッドを呼ぶ
        val chooserIntent = Intent.createChooser(galleryIntent, "画像を取得")

        // EXTRA_INITIAL_INTENTS にカメラ撮影のIntentを追加
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        startActivityForResult(chooserIntent, CHOOSER_REQUEST_CODE)
    }

    // Intent のカメラとかの連携から戻ってきたときにImageViewへ
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CHOOSER_REQUEST_CODE) {

            if (resultCode != Activity.RESULT_OK) {
                if (mPictureUri != null) {
                    contentResolver.delete(mPictureUri!!, null, null)
                    mPictureUri = null
                }
                return
            }

            // 画像を取得
            val uri = if (data == null || data.data == null) mPictureUri else data.data

            // URIからBitmapを取得する
            val image: Bitmap
            try {
                val contentResolver = contentResolver
                val inputStream = contentResolver.openInputStream(uri!!)
                image = BitmapFactory.decodeStream(inputStream)
                inputStream!!.close()
            } catch (e: Exception) {
                return
            }

            // 取得したBimapの長辺を500ピクセルにリサイズする
            val imageWidth = image.width
            val imageHeight = image.height
            val scale = Math.min(500.toFloat() / imageWidth, 500.toFloat() / imageHeight) // (1)

            val matrix = Matrix()
            matrix.postScale(scale, scale)

            val resizedImage = Bitmap.createBitmap(image, 0, 0, imageWidth, imageHeight, matrix, true)

            // BitmapをImageViewに設定する
            imageView.setImageBitmap(resizedImage)

            mPictureUri = null
        }
    }

    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        progressBar.visibility = View.GONE

        if (databaseError == null) {
            finish()
        } else {
            Snackbar.make(findViewById(android.R.id.content), "投稿に失敗しました", Snackbar.LENGTH_LONG).show()
        }
    }

}
