package ru.emkn.p2beer.app.ui

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.*
import ru.emkn.p2beer.app.client.chat.*
import ru.emkn.p2beer.app.client.user.*
import ru.emkn.p2beer.app.client.util.timestampToDate
import ru.emkn.p2beer.app.client.util.wrapText
import java.io.File
import java.util.regex.Pattern

class DialogWindow(
    private val openChat: ChatImpl,
    private val me: Account,
    private val friendConnection: FriendConnection
    ) {

    fun addDialogWindow(
        actionListBox: ActionListBox,
        textGUI: WindowBasedTextGUI
    ) {

        actionListBox.addItem(openChat.toString()) {
            val window: Window = BasicWindow("Dialog Window")
            window.setHints(listOf(Window.Hint.EXPANDED))

            val mainPanel = Panel()
            mainPanel.layoutManager = BorderLayout()

            val chatPanel = Panel()

            /**
             * Time for the second big part of mainPanel
             * It's called chatPanel
             * It's Location is set to center and that's
             * how it takes the whole space of the screen,
             * that is not yet taken by any other elements
             */

            chatPanel.layoutManager = BorderLayout()

            mainPanel.addComponent(
                chatPanel.withBorder(Borders.doubleLineBevel(openChat.toString()))
                    .setLayoutData(BorderLayout.Location.CENTER)
            )

            val messages = TextBox(
                TerminalSize(20, 9)
            ).setLayoutData(
                LinearLayout.createLayoutData(LinearLayout.Alignment.Fill)
            )

            checkIfChatRegistered(openChat.toString())

            val bTree = BTree(5,
                    "src/main/kotlin/ru/emkn/p2beer/app/resources/chatlists/$openChat/index.bin",
                    "src/main/kotlin/ru/emkn/p2beer/app/resources/chatlists/$openChat/messages.bin"
            )

            if (getNumberOfMessages(bTree) != 0) {

                val messageList = getKLastMessages(
                        bTree,
                        minOf(getNumberOfMessages(bTree), 20)
                )

                for (message in messageList.reversed()) {
                    messages.addLine(messageToString(message))
                }
            }

            messages.setValidationPattern(Pattern.compile("/(?:)/"))

            messages.setCaretPosition(messages.lineCount, 0)

            chatPanel.addComponent(
                messages.setLayoutData(BorderLayout.Location.CENTER)
            )

            /**
             * In ChatPanel we need to have a textBox to type th message
             * Its size is defined by the space given to ChatPanel
             * Its stretched out to the whole width of the panel
             */

            val bottomMessageInputBox = Panel()
            bottomMessageInputBox.layoutManager = LinearLayout()

            bottomMessageInputBox.addComponent(
                Separator(Direction.HORIZONTAL)
                    .setLayoutData(
                        GridLayout.createHorizontallyFilledLayoutData(2)
                    )
            )

            /**
             * Initialize btree to efficiently save data
             */

            val messageField = TextBox(
                TerminalSize(20, 3),
                "Type message..."
            ).setLayoutData(
                LinearLayout.createLayoutData(LinearLayout.Alignment.Fill)
            )
            bottomMessageInputBox.addComponent(messageField)

            val sendBtn = Button("Send") {
                val msg = createMessage(messageField.text)
                sendMessage(bTree, msg, messages, messageField)
            }

            bottomMessageInputBox.addComponent(
                sendBtn.setLayoutData(
                    LinearLayout.createLayoutData(LinearLayout.Alignment.End)
                )
            )

            /**
             * messageField is lowered to the bottom
             * of the chatPanel by the function below
             */

            chatPanel.addComponent(
                bottomMessageInputBox.setLayoutData(BorderLayout.Location.BOTTOM)
            )

            val button = Button("Close") {

                /**
                 * Update data about Friend's last message
                 * TODO: Handle situations when the window
                 *  is closed not properly
                 */

                val dataStorage = JSONUserDataStorageImpl()

                for (friend in me.friends) {
                    if (friend.userInfo.pubKey.contentEquals(openChat.friend.userInfo.pubKey)) {
                        friend.lastMessageTimeStamp = openChat.friend.lastMessageTimeStamp
                        friend.messagesCount = openChat.friend.messagesCount
                    }
                }

                dataStorage.saveMyData(me)
                window.close()
            }

            mainPanel.addComponent(
                button.setLayoutData(
                    GridLayout.createHorizontallyEndAlignedLayoutData(2)
                ).setLayoutData(BorderLayout.Location.BOTTOM)
            )

            window.component = mainPanel.withBorder(Borders.singleLine("Main Panel"))
            textGUI.addWindowAndWait(window)

        }
    }

    private fun messageToString(message: Message) : String {
        //TODO: Optimize textBox to resize messages for proper width
        return("""
            |${timestampToDate(message)}
            |   ${wrapText(50, message.text)}
            |"""
                .trimMargin()
                )
        //TODO: Change colors of field Sender and time

        //TODO: add author to the shown message
    }

    private fun createMessage(message: String) : Message {
        val pk = me.userInfo.pubKey
        val twoBytesOfUserID : UShort = (pk[pk.size - 1] + pk[pk.size - 2]).toUShort()
        val info = MessageId(
                openChat.friend.messagesCount + 1,
                System.currentTimeMillis(),
                twoBytesOfUserID
        )
        return Message(message, info, pk)
    }

    private fun sendMessage(bTree: BTree,
                            message: Message,
                            messages: TextBox,
                            messageField: TextBox
    ) {
        /**
         * Send message
         */

        // TODO: add call of the function that sends a message
        //  and save the message on success

        /**
         * Save message to file
         */

        addMessage(bTree, message)

        /**
         * Update the time of the last message in the
         */

        openChat.friend.lastMessageTimeStamp = System.currentTimeMillis()

        /**
         * Update amount of messages sent to this friend
         */

        openChat.friend.messagesCount += 1

        /**
         * The ability of modifying the textField
         * with messages is blocked by default, so we
         * disable it for the time of adding a new
         * message
         */

        messages.setValidationPattern(null)

        /**
         * Show this message
         * as new in chat
         */

        messages.addLine(messageToString(message))
        messages.setCaretPosition(messages.lineCount, 0)
        messageField.text = ""

        /**
         * And block it again
         */

        messages.setValidationPattern(Pattern.compile("/(?:)/"))
    }
}

fun checkIfChatRegistered(userName: String) {
    val folderName = "src/main/kotlin/ru/emkn/p2beer/app/resources/chatlists/$userName/"
    if (!File(folderName).exists())
        File(folderName).mkdirs()
}

