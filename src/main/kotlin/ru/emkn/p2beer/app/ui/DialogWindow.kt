package ru.emkn.p2beer.app.ui

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.*
import ru.emkn.p2beer.app.client.chat.*
import ru.emkn.p2beer.app.client.user.Account
import ru.emkn.p2beer.app.client.user.UserInfo
import ru.emkn.p2beer.app.client.util.timestampToDate
import ru.emkn.p2beer.app.client.util.wrapText
import java.io.File
import kotlin.random.Random
import kotlin.random.nextUInt

class DialogWindow(
    private val info: ChatImpl,
    private val me: Account
    ) {

    fun addDialogWindow(
        actionListBox: ActionListBox,
        textGUI: WindowBasedTextGUI
    ) {

        actionListBox.addItem(info.toString()) {
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
                chatPanel.withBorder(Borders.doubleLineBevel(info.toString()))
                    .setLayoutData(BorderLayout.Location.CENTER)
            )

            val messages = TextBox(
                TerminalSize(20, 9)
            ).setLayoutData(
                LinearLayout.createLayoutData(LinearLayout.Alignment.Fill)
            )

            checkIfChatRegistered(info.toString())

            val bTree = BTree(5,
                    "src/main/kotlin/ru/emkn/p2beer/app/resources/chatlists/$info/index.bin",
                    "src/main/kotlin/ru/emkn/p2beer/app/resources/chatlists/$info/messages.bin"
            )

            val pk = Random.nextBytes(32)
            val uid = Random.nextUInt(0u, UShort.MAX_VALUE + 1u).toUShort()
            val time = System.currentTimeMillis() - Random.nextInt(0, 5)
            val infoM = MessageId(Random.nextLong(0, 5), time, uid)

            for (i in 1 until 30)
                messages.addLine(
                    messageToString(
                        Message(
                            "Lorem ipsum dolor sit amet, " +
                                    "consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna " +
                                    "aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip " +
                                    "ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse " +
                                    "cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, " +
                                    "sunt in culpa qui officia deserunt mollit anim id est laborum.", infoM, pk
                        )
                    )
                )

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

                /**
                 * Send message
                 */

                // TODO: add call of the function that sends a message
                //  and save the message on success

                /**
                 * Save message
                 */

                addMessage(bTree, createMessage(messageField.text))
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

            mainPanel.addComponent(
                Button("Close", window::close).setLayoutData(
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
    }

    private fun createMessage(message: String) : Message {
        val pk = me.userInfo.pubKey
        val twoBytesOfUserID : UShort = (pk[pk.size - 1] + pk[pk.size - 2]).toUShort()
        val info = MessageId(
            1,
            System.currentTimeMillis(),
            twoBytesOfUserID)
        return Message(message, info, pk)
    }
}

fun checkIfChatRegistered(userName: String) {
    val folderName = "src/main/kotlin/ru/emkn/p2beer/app/resources/chatlists/$userName/"
    if (!File(folderName).exists())
        File(folderName).mkdirs()
}