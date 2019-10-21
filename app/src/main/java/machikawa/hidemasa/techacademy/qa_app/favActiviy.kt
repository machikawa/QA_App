package machikawa.hidemasa.techacademy.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ListView
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class favActiviy : AppCompatActivity() {

    private var mGenre = 0
    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mUserFavsReference: DatabaseReference
    private lateinit var mListView: ListView
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter

    private val mFavoriteListner = object : ChildEventListener {
        // お気に入り取得
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            mDatabaseReference.child(ContentsPATH)
                .child(dataSnapshot.value.toString()) //ジャンル
                .child(dataSnapshot.key.toString()) // Questuin UID
                .addListenerForSingleValueEvent(
                    object : ValueEventListener
                    {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val map = snapshot.value as Map<String, String>
                        val title = map["title"] ?: ""
                        val body = map["body"] ?: ""
                        val name = map["name"] ?: ""
                        val uid = map["uid"] ?: ""
                        val imageString = map["image"] ?: ""
                        val bytes =
                            if (imageString.isNotEmpty()) {
                                Base64.decode(imageString, Base64.DEFAULT)
                            } else {
                                byteArrayOf()
                            }
                        val answerArrayList = ArrayList<Answer>()
                        val answerMap = map["answers"] as Map<String, String>?
                        if (answerMap != null) {
                            for (key in answerMap.keys) {
                                val temp = answerMap[key] as Map<String, String>
                                val answerBody = temp["body"] ?: ""
                                val answerName = temp["name"] ?: ""
                                val answerUid = temp["uid"] ?: ""
                                val answer = Answer(answerBody, answerName, answerUid, key)
                                answerArrayList.add(answer)
                            }
                        }
                        val question = Question(
                            title, body, name, uid, snapshot.key ?: "",
                            mGenre, bytes, answerArrayList
                        )
                        mQuestionArrayList.add(question)
                        mAdapter.notifyDataSetChanged() // Listviewのさい描写
                    }
                    override fun onCancelled(firebaseError: DatabaseError) {
                    }
                })
        }
        // 回答を追加するときに起動する。これがないと回答が追加されない
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>
            // 変更があったQuestionを探す
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    // このアプリで変更がある可能性があるのは回答(Answer)のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }
                    mAdapter.notifyDataSetChanged()
                }
            }
        }
        override fun onChildRemoved(p0: DataSnapshot) {
        }
        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
        }
        override fun onCancelled(p0: DatabaseError) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fav_activiy)

        // タイトル
        title = getString(R.string.favorite)
        // DB ref の宣言
        mDatabaseReference = FirebaseDatabase.getInstance().reference
        // リストビュー定義
        mListView = findViewById(R.id.favListView)
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        // リストビューを弾くと当該の問題に繊維
        mListView.setOnItemClickListener { parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }
    }

    // 詳細画面から戻ってきた時の処理。再度 List View を描写する
    override fun onResume() {
        super.onResume()
        // ログインしているユーザーの取得
        val currentUser = FirebaseAuth.getInstance().currentUser
        // 一度クリアーし、再度クエッションのアレイリストをアダプターにセット
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        mListView.adapter = mAdapter
        // Firebase のインスタンスを生成お気に入り/userの配下
        mUserFavsReference = mDatabaseReference.child(favoritesMgmtPath).child(currentUser!!.uid.toString())
        mUserFavsReference!!.addChildEventListener(mFavoriteListner)
    }
}
