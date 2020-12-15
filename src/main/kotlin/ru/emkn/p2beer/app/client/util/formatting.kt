package ru.emkn.p2beer.app.client.util

import ru.emkn.p2beer.app.client.chat.Message
import java.text.SimpleDateFormat
import java.util.*


val simpleDateFormat = SimpleDateFormat("dd MMMM, HH:mm", Locale.ENGLISH)

fun timestampToDate (message: Message) : String = simpleDateFormat.format(Date(message.info.timestamp))

fun wrapText(textviewWidth : Int, msg: String) : String {
    var temp = ""
    var sentence = ""
    // split by space
    val arrayNeeded = msg.split(" ")

    for (word in arrayNeeded) {
        if ((temp.length + word.length) < textviewWidth) {
            temp += " $word"

        } else {
            // add new line character
            sentence += "$temp\n   "
            temp = word
        }

    }

    return (sentence.replaceFirst(" ", "")+temp)
}