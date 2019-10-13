package machikawa.hidemasa.techacademy.qa_app

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCreateAccountListner : OnCompleteListener<AuthResult>
    private lateinit var mLoginListner : OnCompleteListener<AuthResult>
    private lateinit var mDataBaseReference: DatabaseReference

    // アカウントを作成したことを示すもの。Firebaseへのログイン処理後の保存に使う模様
    private  var mIsCreateAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // DataBase reference って何？
        mDataBaseReference = FirebaseDatabase.getInstance().reference

        // Firebase auth のオブジェクト
        mAuth = FirebaseAuth.getInstance()

        // アカウント作成のリスナー
        mCreateAccountListner = OnCompleteListener {task ->
            if (task.isSuccessful) {
                val email = emailText.text.toString()
                val password = passwordText.text.toString()
                login(email,password)
            } else {

                // 失敗した場合
                // エラーを表示する
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, "アカウント作成に失敗しました", Snackbar.LENGTH_LONG).show()
                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        mLoginListner = OnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = mAuth.currentUser
                var userRef = mDataBaseReference.child(UsersPATH).child(user!!.uid)

                if (mIsCreateAccount) {
                    val name = nameText.text.toString()
                    val data = HashMap<String, String>()
                    data["name"] = name
                    userRef.setValue(data)

                    // 表示名をPreferenceへ保存
                    saveName(name)
                } else {
                    // ログイン処理になるかな。
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val data = snapshot.value as Map<*, *>?
                            saveName(data!!["name"] as String)
                        }
                        override fun onCancelled(p0: DatabaseError) {}
                    })
                }
                //ここでプログレスバーの非表示
                progressBar.visibility = View.GONE
                // アクティビティ終了
                finish()

                // ログインの失敗時
            } else {
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, "ログインに失敗しました", Snackbar.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
            }
        }


        // タイトルの設定
        title = "ログイン"

        createButton.setOnClickListener { v ->
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()
            val name = nameText.text.toString()

            // 3つきっちり揃っていたらクリエイト、満たしてなければ出直せと注意。
            if (email.length != 0 && password.length >= 6 && name.length != 0) {
                // ログイン時に表示名を保存するようにフラグを立てる
                mIsCreateAccount = true
                createAccount(email, password)
            } else {
                // エラーを表示する
                Snackbar.make(v, "正しく入力してください", Snackbar.LENGTH_LONG).show()
            }
        }

        loginButton.setOnClickListener { v ->
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()

            if (email.length != 0 && password.length >= 6) {
                // フラグを落としておく
                mIsCreateAccount = false

                login(email, password)
            } else {
                // エラーを表示する
                Snackbar.make(v, "正しく入力してください", Snackbar.LENGTH_LONG).show()
            }
        }

    }///// Oncreate 終わり
    
    private fun createAccount(email: String, password: String) {
        // プログレスバーを表示する
        progressBar.visibility = View.VISIBLE
        // アカウントを作成する
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(mCreateAccountListner)
    }
    private fun login(email: String, password: String) {
        // プログレスバーを表示する
        progressBar.visibility = View.VISIBLE

        // ログインする
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(mLoginListner)
    }
    private fun saveName(name: String) {
        // Preferenceに保存する
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sp.edit()
        editor.putString(NameKEY, name)
        editor.commit()
    }
} /// クラス終わり
