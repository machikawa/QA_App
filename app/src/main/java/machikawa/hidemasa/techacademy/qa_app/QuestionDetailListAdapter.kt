package machikawa.hidemasa.techacademy.qa_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class QuestionDetailListAdapter(
    context: Context,
    private val mQustion: Question
    ) : BaseAdapter() {

    companion object {
        private val TYPE_QUESTION = 0
        private val TYPE_ANSWER = 1
    }

    private var mLayoutInfrater : LayoutInflater? = null

    // レイアウトインフレーターはプログラムでレイアウトのデータ使うための人。
    init {
        mLayoutInfrater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as  LayoutInflater
    }

    // 質問一覧の画面のリストビューでアイテムをタップしたときに起動する模様。
    // 回答を投稿したあとにも動作しているようだ。
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var convertView = convertView

        if (getItemViewType(position) == TYPE_QUESTION){
            if (convertView == null) {
                convertView = mLayoutInfrater!!.inflate(R.layout.list_question_detail,parent,false)!!
            }
            val body = mQustion.body
            val name = mQustion.name

            val bodyTextView = convertView.findViewById<View>(R.id.bodyTextView) as TextView
            bodyTextView.text = body

            val nameTextView = convertView.findViewById<View>(R.id.nameTextView) as TextView
            nameTextView.text = name

            val bytes = mQustion.imageBytes
            if (bytes.isNotEmpty()) {
                val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).copy(Bitmap.Config.ARGB_8888, true)
                val imageView = convertView.findViewById<View>(R.id.imageView) as ImageView
                imageView.setImageBitmap(image)
            }
        } else {
            if (convertView == null) {
                convertView = mLayoutInfrater!!.inflate(R.layout.list_answer,parent,false)!!
            }

            val answer = mQustion.answers[position - 1]
            val body = answer.body
            val name = answer.name

            val bodyTextView = convertView.findViewById<View>(R.id.bodyTextView) as TextView
            bodyTextView.text = body

            val nameTextView = convertView.findViewById<View>(R.id.nameTextView) as TextView
            nameTextView.text = name
        }
        return convertView
    }

    // うまくコールされていいない気がする
    override fun getItem(position: Int): Any {
        Log.d("machid","QuestionDetailListAdapter-GETTTEM")
        return mQustion
    }

    // タップしたポジション？な気がする。詳細行をタップするたびにコールされている。
    override fun getItemId(position: Int): Long {
        return 0
    }

    // 純粋にリストビューの行を表示しているように見える。
    override fun getCount(): Int {
        return 1 + mQustion.answers.size
    }

    // おそらくリストビューのレコードの分だけ出ている希ガス。
    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            TYPE_QUESTION
        } else {
            TYPE_ANSWER
        }
    }
}