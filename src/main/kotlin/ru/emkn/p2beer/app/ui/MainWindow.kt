package ru.emkn.p2beer.app.ui

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import ru.emkn.p2beer.app.client.chat.ChatImpl
import ru.emkn.p2beer.app.client.user.Account
import ru.emkn.p2beer.app.client.user.FriendComparator
import ru.emkn.p2beer.app.client.user.FriendsManager
import ru.emkn.p2beer.app.p2bLib.FriendsManagerImpl
import java.io.IOException

class MainWindow (private val me: Account) {
    fun runMainWindow() {
        try {
            /**
             * Default screen creating
             */
            val screen = DefaultTerminalFactory().createScreen()
            screen.startScreen()

            val textGUI: WindowBasedTextGUI = MultiWindowTextGUI(screen, TextColor.ANSI.CYAN)

            val window: Window = BasicWindow("Dialog Window")

            /**
             * Creating a Panel with a BorderLayout
             * mainPanel consists of three sub-elements which are resizable
             * due to being non pre-sized
             */

            val mainPanel = Panel()
            mainPanel.layoutManager = BorderLayout()

            /**
             * The first sub-element of the mane Panel is a chatList panel
             * It uses a Linear Layout so that the list of dialogs with friends
             * is easy to customize and operate with
             */

            val chatListPanel = Panel()

            /**
             * Here LinearLayout.Alignment is set to 'Fill" cause the list
             * of dialogs must have the same height as the window
             */

            chatListPanel.layoutData = LinearLayout.createLayoutData(LinearLayout.Alignment.Fill);

            /**
             * The main component of chatListPanel is, as
             * was already told - dialogs
             * They are presented as items in an ActionListBox
             * @see ActionListBox
             * Each item is provided with an action triggered when the
             * item is chosen
             * The width of [ActionListBox] is set to 20 by default
             * cause it allows to structure the other parts of the window
             * and align them around it
             */

            /**
             * Create an instance of the [FriendsManager]
             */

            val friendsManager = FriendsManagerImpl()

            val actionListBox = ActionListBox(TerminalSize(20, 10))
            var chat : DialogWindow
            for (friend in me.friends.sortedWith(FriendComparator)) {

                /**
                 * Get connection to friend via FriendsManager
                 */

                chat = DialogWindow(
                        ChatImpl(friend),
                        me,
                        friendsManager.getConnectionTo(friend)
                )
                chat.addDialogWindow(actionListBox, textGUI)
            }

            chatListPanel.addComponent(actionListBox)

            /**
             * Here we locate our freshly created chatListPanel
             * on the BorderLayout of mainPanel
             * Now it will be sticked to the left side of the screen
             */

            mainPanel.addComponent(
                    chatListPanel.withBorder(Borders.doubleLineBevel("Chat list"))
                            .setLayoutData(BorderLayout.Location.LEFT)
            )

            /**
             * Last, but not least, a tiny 'exit' button
             * is located at the bottom of the BorderLayout
             * This way it goes right below both panels
             * with messages and chats
             */

            mainPanel.addComponent(
                    Button("Close", window::close).setLayoutData(
                            GridLayout.createHorizontallyEndAlignedLayoutData(2)
                    ).setLayoutData(BorderLayout.Location.BOTTOM)
            )

            /**
             * Make the window active
             */

            window.component = mainPanel.withBorder(Borders.singleLine("Main Panel"))
            textGUI.addWindowAndWait(window)

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

