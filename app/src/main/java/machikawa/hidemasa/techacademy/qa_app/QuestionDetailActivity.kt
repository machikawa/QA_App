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

    ////// 課題関連 START
    // お気に入りにしているかどうか
    var isFavorite:Boolean = false
    private lateinit var mfavRef:DatabaseReference
    ////// 課題関連 END

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

    ////// 課題関連 START
    // おきに追加のイベントリスナー
    private val mFavoriteEventListener = object : ChildEventListener {
        override fun onCancelled(p0: DatabaseError) {
        }
        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
        }
        override fun onChildChanged(p0: DataSnapshot, p1: String?) {
        }
        // お気に入りの追加が押さたときの処理として
        override fun onChildAdded(p0: DataSnapshot, p1: String?) {
            // 画面ロード時に当該ユーザーのお気に入りQuesitonID一覧が読み込まれるため、現在のQuestionUIDのものがあるか判断する
            val favQuestionUid = p0.key
            if (favQuestionUid == mQuestion.questionUid) {
                favoriteBtn.setTextColor(Color.parseColor("#FFD700"))
                isFavorite = true
            }
        }
        // Remove 時の処理は EventListern にて実施する。
        override fun onChildRemoved(p0: DataSnapshot) {
        }
    }
    ////// 課題関連 END

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        title = mQuestion.title

        mAdapter = QuestionDetailListAdapter(this,mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser

        fab.setOnClickListener {
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

        ////// 課題関連 STRAT
        // お気に入り機能の表示有無　ログイン時のみ表示する
        if (user != null) {
            favoriteBtn.visibility = View.VISIBLE
        } else {
            favoriteBtn.visibility = View.INVISIBLE
        }

        // こちらも全てログイン時のみに行われる処理。うまく分離するなりマージしなくてはいけないが。。
        if (user != null) {

            //////// この辺りから　Favorite 関連を処理しているつもり
            val favDBRef= FirebaseDatabase.getInstance().reference
            mfavRef = favDBRef.child(favoritesMgmtPath).child(user!!.uid.toString())
            mfavRef.addChildEventListener(mFavoriteEventListener)

            //ボタンタップでオキニ削除or登録.
            favoriteBtn.setOnClickListener {
                val dbref = FirebaseDatabase.getInstance().reference.child(favoritesMgmtPath)
                    .child(user!!.uid.toString()).child(mQuestion.questionUid)
                val mapper = HashMap<String, String>()
                mapper["genre"] = mQuestion.genre.toString()
                if (isFavorite) {
                    dbref.removeValue()
                    isFavorite = false
                    favoriteBtn.setTextColor(Color.parseColor("#DCDCDC"))
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "お気に入りから削除されました",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    dbref.setValue(mapper)
                    isFavorite = true
                    favoriteBtn.setTextColor(Color.parseColor("#FFD700"))
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "お気に入りに追加されました",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
        ////// 課題関連 END
    }
}