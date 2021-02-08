package ru.emkn.p2beer.app.ui

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.TextBox
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.WindowListener
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import ru.emkn.p2beer.app.client.chat.*
import ru.emkn.p2beer.app.client.util.messagesLoadByOnceNum
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
/*

class MessagesScrollListener(
        private val textBox: TextBox,
        private val bTree: BTree,
        private val chat: ChatImpl
) : WindowListener {
    override fun onInput(basePane: Window?, keyStroke: KeyStroke, deliverEvent: AtomicBoolean?) {
        if (keyStroke.keyType == KeyType.ArrowUp)
            if (textBox.isFocused && textBox.caretPosition.row < 5)
                loadNewMessages()
    }

    override fun onUnhandledInput(basePane: Window?, keyStroke: KeyStroke?, hasBeenHandled: AtomicBoolean?) {
        // TODO Auto-generated method stub
    }

    override fun onResized(window: Window?, oldSize: TerminalSize?, newSize: TerminalSize?) {
        // TODO Auto-generated method stub
    }

    override fun onMoved(window: Window?, oldPosition: TerminalPosition?, newPosition: TerminalPosition?) {
        // TODO Auto-generated method stub
    }

    private fun loadNewMessages() {
        if (getNumberOfMessages(bTree) > chat.loadedMessagesCount) {
            val messageList = getKPreviousMessages(
                    bTree,
                    chat.firstLoadedMessage,
                    minOf(messagesLoadByOnceNum)
            )

            chat.firstLoadedMessage = messageList.last()
            chat.loadedMessagesCount += messageList.size
            val text = textBox.text
            val maximumCaretPosition = textBox.lineCount

            var newText = ""

            for (element in messageList.reversed()) {
                newText += messageToString(element) + "\n"
            }

            newText += text

            textBox.setValidationPattern(null)
            textBox.text = newText
            textBox.setValidationPattern(Pattern.compile("/(?:)/"))

            textBox.setCaretPosition(textBox.lineCount - maximumCaretPosition + 5, 0)
        }
    }
}

 */