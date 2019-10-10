package machikawa.hidemasa.techacademy.qa_app

import java.io.Serializable
import java.util.ArrayList

class Question
    (
    val title: String,
    val body: String,
    val name: String,
    val uid: String,
    val questionUid: String,
    val genre: Int,
    bytes: ByteArray, // Image をバイト型にして表現したもの。
    val answers: ArrayList<Answer>
    ) : Serializable {  // Serializable なのは Intent でデータを渡すようにするため。

    val imageBytes: ByteArray
    //
    init {
        imageBytes = bytes.clone()
    }
}
