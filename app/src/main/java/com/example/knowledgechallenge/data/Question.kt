package com.example.knowledgechallenge.data

import kotlin.random.Random

data class Question(
    val category: Category,
    val text: String,
    val options: List<String>,
    val correctIndex: Int
) {
    /** نسخة بترتيب خيارات عشوائي أو ثابت حسب البذرة مع تصحيح فهرس الإجابة الصحيحة. */
    fun withShuffledOptions(random: Random = Random.Default): Question {
        val correct = options[correctIndex]
        val shuffled = options.shuffled(random)
        return copy(options = shuffled, correctIndex = shuffled.indexOf(correct))
    }
}
