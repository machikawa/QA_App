package machikawa.hidemasa.techacademy.qa_app

import android.content.Context
import android.view.LayoutInflater
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class QuestionsListAdapter(context: Context):BaseAdapter() {

    private var mLayoutInflater: LayoutInflater
    private var mQuestionArrayList = ArrayList<Question>()
    init {
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    // 実装必須なメソッドたち
    // なぜかコールされていない。
    override fun getItem(position: Int): Any {
        return mQuestionArrayList[position]
    }
    // 質問一覧リストビューのタップしたアイテムのポジションを返すっぽい。
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
    // 質問一覧画面を表示したときに１行あたり２回なぜかコールされており、最終行だけはなぜか１回。
    override fun getCount(): Int {
        return mQuestionArrayList.size
    }
    // 実装必須の中で最長
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var convertView = convertView

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.list_questions, parent, false)
        }

        // タップしたポジションから諸々引っ張ってくる〜。
        val titleText = convertView!!.findViewById<View>(R.id.titleTextView) as TextView
        titleText.text = mQuestionArrayList[position].title

        val nameText = convertView.findViewById<View>(R.id.nameTextView) as TextView
        nameText.text = mQuestionArrayList[position].name

        val resText = convertView.findViewById<View>(R.id.resTextView) as TextView
        val resNum = mQuestionArrayList[position].answers.size
        resText.text = resNum.toString()

        val bytes = mQuestionArrayList[position].imageBytes
        if (bytes.isNotEmpty()) {
            val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).copy(Bitmap.Config.ARGB_8888, true)
            val imageView = convertView.findViewById<View>(R.id.imageView) as ImageView
            imageView.setImageBitmap(image)
        }
        return convertView
    }

    // 質問一覧のあれーりすと.　このメソッドではコールしていなくてMain アクティビティで使っている。
    fun setQuestionArrayList(questionArrayList: ArrayList<Question>) {
        mQuestionArrayList = questionArrayList
    }

}