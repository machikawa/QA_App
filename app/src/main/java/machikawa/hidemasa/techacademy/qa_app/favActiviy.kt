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

    private lateinit var mToolbar: Toolbar
    private var mGenre = 0
    private var mfavQuestionUidArrayList = arrayListOf<String>()

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mUserFavsReference: DatabaseReference
    private lateinit var mListView: ListView
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter
    private var mGenreRef: DatabaseReference? = null

    private val mEventListener = object : ChildEventListener {
        // 質問を追加したりするときに起動する模様
        // 質問を追加したりするときに起動する模様
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>
            val title = map["title"] ?: ""
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""
            val imageString = map["image"] ?: ""
            val bytes =
                if (imageString.isNotEmpty()) {
                    Base64.decode(imageString, Base64.DEFAULT)
                } else {
                    // なんか適当なByteArray返す。https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/byte-array-of.html
                    byteArrayOf()
                }
            // 回答のハッシュマップ。質問1つに対し諸々つくからこうなるのだと思う。
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

            // お気に入りアレイのQuestionUIDと全QuestionのQuestionUIDの比較
            for (fav in mfavQuestionUidArrayList ) {
                if (fav == uid) {
                    val question = Question(
                        title, body, name, uid, dataSnapshot.key ?: "",
                        mGenre, bytes, answerArrayList
                    )
                    mQuestionArrayList.add(question)
                    Log.d("machid", "mEve-ONChild")
                    mAdapter.notifyDataSetChanged()
                }
            }

        }

        // 回答を追加するときに起動する模様
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
        }
        override fun onChildRemoved(p0: DataSnapshot) {
        }
        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
        }
        override fun onCancelled(p0: DatabaseError) {
        }
    }

    private val mFavoriteListner = object : ChildEventListener {
        // 質問を追加したりするときに起動する模様
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val favQuestionUid = dataSnapshot.key
            mfavQuestionUidArrayList.add(favQuestionUid!!)
        }
        // 回答を追加するときに起動する模様
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
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

        // ログインしているユーザーの取得
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Firebase のインスタンスを生成お気に入り/userの配下
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        mListView = findViewById(R.id.favListView)
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        mUserFavsReference = mDatabaseReference.child(favoritesMgmtPath).child(currentUser!!.uid.toString())
        Log.d("machid","UDI=" + currentUser.uid.toString())
        mUserFavsReference!!.addChildEventListener(mFavoriteListner)

        mGenreRef = mDatabaseReference.child(ContentsPATH)
        mGenreRef!!.addChildEventListener(mEventListener)

  //      mQuestionArrayList.clear()
  //      mAdapter.setQuestionArrayList(mQuestionArrayList)
  //      mListView.adapter = mAdapter

        // リストビューを弾くと当該の問題に繊維
        mListView.setOnItemClickListener { parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }
        Log.d("machid", "QuestionSize="+mQuestionArrayList.size.toString())
        Log.d("machid", "favsize="+mfavQuestionUidArrayList.size.toString())
    }

    // 詳細画面から戻ってきた時の処理。再度 List View を描写する
    override fun onResume() {
        super.onResume()


    }

}
