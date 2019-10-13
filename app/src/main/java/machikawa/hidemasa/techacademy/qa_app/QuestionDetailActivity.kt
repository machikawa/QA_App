package machikawa.hidemasa.techacademy.qa_app

import android.bluetooth.BluetoothA2dp
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

class QuestionDetailActivity : AppCompatActivity() {

    // お気に入りにしているかどうか
    var isFavorite:Boolean = false

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference

    // アンサー追加のイベントリすな〜。
    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s:String?){
            val map = dataSnapshot.value as Map<String, String>
            val answerUid = dataSnapshot.key ?: ""
            for (answer in mQuestion.answers) {
                if (answerUid == answer.answerUid) {
                    return
                }
            }
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }
        override fun onCancelled(p0: DatabaseError) {
        }
        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
        }
        override fun onChildChanged(p0: DataSnapshot, p1: String?) {
        }
        override fun onChildRemoved(p0: DataSnapshot) {
        }
    }

    // おきに追加のイベントリスナー
    private val mFavoriteEventListener = object : ChildEventListener {
        override fun onCancelled(p0: DatabaseError) {
        }
        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
        }
        override fun onChildChanged(p0: DataSnapshot, p1: String?) {
        }
        // お気に入りの追加が押されたなら
        override fun onChildAdded(p0: DataSnapshot, p1: String?) {
            isFavorite = true
//            favoriteBtn.text = "お気に入り済み"
            favoriteBtn.setTextColor(Color.parseColor("#FFD700"))
        }
        // お気に入りがもう一度押されたなら
        override fun onChildRemoved(p0: DataSnapshot) {
            isFavorite = false
            favoriteBtn.setTextColor(Color.parseColor("#DCDCDC"))
//            favoriteBtn.text = "ブクマしてくれ"
        }
    }

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        title = mQuestion.title

        mAdapter = QuestionDetailListAdapter(this,mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }
        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)
    }

    // お気に入りボタンの表示判定。ログインして帰ってきたときなどのために OnResume に定義している。
    override fun onResume() {
        super.onResume()

        val loginuser = FirebaseAuth.getInstance().currentUser
        if (loginuser != null) {
            favoriteBtn.visibility = View.VISIBLE
        } else {
            favoriteBtn.visibility = View.INVISIBLE
        }
        //ボタンタップでオキニ削除or登録
        favoriteBtn.setOnClickListener{
            val dbref = FirebaseDatabase.getInstance().reference.child(favoritesMgmtPath).child(loginuser!!.uid.toString()).child(mQuestion.questionUid)
            dbref.addChildEventListener(mFavoriteEventListener)
            val mapper = HashMap<String, String>()
            mapper["genre"] = mQuestion.genre.toString()
            if (isFavorite) {
                dbref.removeValue()
                Snackbar.make(findViewById(android.R.id.content),"お気に入りから削除されました",Snackbar.LENGTH_SHORT).show()
            } else {
                dbref.setValue(mapper)
                Snackbar.make(findViewById(android.R.id.content),"お気に入りに追加されました",Snackbar.LENGTH_SHORT).show()
            }
//// 【オフにし忘れるな！！！】データ完全削除用 オキニボタンの押下でデータ全削除
/////            dbref.setValue(mapper)
        }
    }
}
