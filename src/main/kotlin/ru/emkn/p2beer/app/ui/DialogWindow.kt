package ru.emkn.p2beer.app.ui

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.*
import ru.emkn.p2beer.app.client.chat.ChatImpl
import java.util.regex.Pattern

class DialogWindow (private val info : ChatImpl) {
    fun addDialogWindow(
        actionListBox : ActionListBox,
        textGUI : WindowBasedTextGUI,
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

            for (i in 1 until 30)
                messages.addLine("$i.12.20                 Привет!")

            messages.setCaretPosition(messages.lineCount,0)

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


            val messageField = TextBox(
                TerminalSize(20, 3),
                "Type message..."
            ).setLayoutData(
                LinearLayout.createLayoutData(LinearLayout.Alignment.Fill)
            )
            bottomMessageInputBox.addComponent(messageField)

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
}