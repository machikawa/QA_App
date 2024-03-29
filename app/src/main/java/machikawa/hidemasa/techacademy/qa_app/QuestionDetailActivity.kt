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
    val favoritedBtnColor:String = "#FFD700"
    val notFavoritedBtnColor:String = "#DCDCDC"
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
                favoriteAction()
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
        // ログイン時の処理
        // お気に入りボタン表示、お気に入りボタンのリスナー処理登録
        if (user != null) {
            favoriteBtn.visibility = View.VISIBLE

            val userFavsDBRef= FirebaseDatabase.getInstance().reference
            mfavRef = userFavsDBRef.child(favoritesMgmtPath).child(user!!.uid.toString())
            mfavRef.addChildEventListener(mFavoriteEventListener)

            //ボタンタップでオキニ削除or登録.
            favoriteBtn.setOnClickListener {
                val dbRef = FirebaseDatabase.getInstance().reference.child(favoritesMgmtPath)
                    .child(user!!.uid.toString()).child(mQuestion.questionUid)
                val mapper = mQuestion.genre

                if (isFavorite) {
                    dbRef.removeValue()
                    undoFavoriteAction()
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "お気に入りから削除されました",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    dbRef.setValue(mapper)
                    favoriteAction()
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "お気に入りに追加されました",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            favoriteBtn.visibility = View.INVISIBLE
        }
        ////// 課題関連 END
    }

        // オキニフラグを反転させて、ボタンの色を変える
    fun favoriteAction (){
        isFavorite = true
        favoriteBtn.setTextColor(Color.parseColor(favoritedBtnColor))
    }
    fun undoFavoriteAction (){
        isFavorite = false
        favoriteBtn.setTextColor(Color.parseColor(notFavoritedBtnColor))
    }
}