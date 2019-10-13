package machikawa.hidemasa.techacademy.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import android.widget.ListView
import com.google.android.material.internal.NavigationMenu
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(),NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mToolbar: Toolbar
    private var mGenre = 0

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mListView: ListView
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter

    private var mGenreRef: DatabaseReference? = null

    //
    private val mEventListener = object : ChildEventListener {
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
            val question = Question(title, body, name, uid, dataSnapshot.key ?: "",
                mGenre, bytes, answerArrayList)
            mQuestionArrayList.add(question)
            mAdapter.notifyDataSetChanged() // ???これが別でも出たが少々謎。
        }

        // 回答を追加するときに起動する模様
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
        setContentView(R.layout.activity_main)
        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)

        // fab は floating action button の略
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        fab.setOnClickListener { view ->
            // ジャンルを選択せずにfab(質問追加)しようとしたときの処理
            if (mGenre == 0) {
                Snackbar.make(view, "ジャンルを選択して下さい", Snackbar.LENGTH_LONG).show()
            } else {
            }

            // 現在のログイン済みユーザーの名称を取得できるかな
            val user = FirebaseAuth.getInstance().currentUser
            // できなければNotYetログインだから別画面へ
            if (user == null) {
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // ジャンルを渡して質問作成画面を起動する
                val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                intent.putExtra("genre", mGenre)
                startActivity(intent)
            }
        }

        // Navigation Drawer 設定用の処理
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(this,drawer,mToolbar,R.string.app_name,R.string.app_name)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        // R.id.Nav_view は Activity Main.xml に定義したナビゲーションビューですので、これをタッチしたときににゃんにゃんする用。
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
        // ???このあたりはタップした時用の処理とは思うが今ひとつ理由もわからず。ナビゲーションビューとドロワーの違いは明らかにせねば。。
        // Firebase のインスタンスを生成
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        mListView = findViewById(R.id.listView)
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        mListView.setOnItemClickListener { parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }
    }

    /// ? これっていつコールされるのだろう。
    override fun onResume() {
        super.onResume()
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val navigationMenu = navigationView.menu.findItem(R.id.nav_favorite)

        // 1:趣味を既定の選択とする
        if(mGenre == 0) {
            onNavigationItemSelected(navigationView.menu.getItem(0))
        }

        // ログイン時のみオキニメニューを表示
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            Log.d("machid", "userNONNULL")
            navigationMenu.setVisible(true)
        } else {
            Log.d("machid", "userNULL")
            navigationMenu.setVisible(false)
        }
    }

    // 最初からあるメソッドたち。
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        // 設定画面に進むためのIntent
        if (id==R.id.action_settings) {
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // ナビビューをセレクトしたときのりすなあ。
    // OnNavigationItemSelectedListener を継承するときに必要
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.nav_hobby) {
            mToolbar.title = "趣味"
            mGenre = 1
        } else if (id == R.id.nav_life) {
            mToolbar.title = "生活"
            mGenre = 2
        } else if (id == R.id.nav_health) {
            mToolbar.title = "健康"
            mGenre = 3
        } else if (id == R.id.nav_compter) {
            mToolbar.title = "コンピューター"
            mGenre = 4
        } else if (id == R.id.nav_protein) {
            mToolbar.title = "筋トレ"
            mGenre = 5
        } else if (id == R.id.nav_favorite){
            /// おきに画面への繊維処理
        }

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)

        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        mListView.adapter = mAdapter

        // 選択したジャンルにリスナーを登録する
        if (mGenreRef != null) {
            mGenreRef!!.removeEventListener(mEventListener)
        }
        mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())
        mGenreRef!!.addChildEventListener(mEventListener)
        return true
    }
}
