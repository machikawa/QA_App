package machikawa.hidemasa.techacademy.qa_app

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.PreferenceManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_setting.*
import android.widget.EditText

class SettingActivity : AppCompatActivity() {

    private lateinit var mDataBaseReference :DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // Preference からの表示名の取得
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val name = sp.getString(NameKEY,"")
        val displayName = findViewById(R.id.nameText) as EditText
        displayName.setText(name)

        mDataBaseReference = FirebaseDatabase.getInstance().reference
        title = "設定"

        // 変更ボタン押下時のリスナーの処理。ログインしてないと怒る。
        changeButton.setOnClickListener {v ->
            // キーボード閉じるよう
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken,InputMethodManager.HIDE_NOT_ALWAYS)

            // ログイン済みのユーザーを取得し
            val user = FirebaseAuth.getInstance().currentUser

            // ログインしてなきゃ帰る
            if (user == null) {
                Snackbar.make(v,"",Snackbar.LENGTH_LONG).show()
                // ログインしてれば各種情報変更
            } else {
                // 表示名変更をFBに！
                val name = nameText.text.toString()
                val userRef = mDataBaseReference.child(UsersPATH).child(user.uid)
                val data = HashMap<String, String>()
                data["name"] = name
                userRef.setValue(data) // ここはHashMap で入れるのと、String の Name を直接入れるのではどう違うのだろうか。

                // SharedPreference にも保存を。ご丁寧にコミットまで
                val sp = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val editor = sp.edit()
                editor.putString(NameKEY, name)
                editor.commit()

                Snackbar.make(v,"表示名を変更しました",Snackbar.LENGTH_LONG).show()
            }
        }

        // ログアウトボタン押下時のリスナーの処理
        logoutButton.setOnClickListener {v ->
            FirebaseAuth.getInstance().signOut()
            nameText.setText("")
            Snackbar.make(v, "ログアウトしました",Snackbar.LENGTH_LONG).show()
        }

    }
}
