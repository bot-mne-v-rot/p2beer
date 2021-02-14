package ru.emkn.p2beer.app.ui

import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.gui2.dialogs.MessageDialog
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import ru.emkn.p2beer.app.client.user.Account
import ru.emkn.p2beer.app.client.user.JSONUserDataStorageImpl
import ru.emkn.p2beer.app.client.user.UserInfo
import ru.emkn.p2beer.app.client.util.*
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import kotlin.random.Random

/**
 * Main entry point.
 */

fun main(args: Array<String>) {

    if (File(userInfoPathJSON).exists()) {

        /**
         * Tries to parse data from JSON file
         * @throws IllegalArgumentException if data
         * is corrupted or invalid and it is impossible to
         * transfer it into class [Account]
         */

        try {
            val mainW = MainWindow(tryToParseUserData())
            mainW.runMainWindow()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    } else
        performLogin()

    /**
     * If file doesn't exist performs login
     */
}

fun performLogin() {
    try {

        val screen = DefaultTerminalFactory().createScreen()
        screen.startScreen()

        val textGUI: WindowBasedTextGUI = MultiWindowTextGUI(screen, TextColor.ANSI.CYAN)

        /**
         * Creating a new window
         */

        val window: Window = BasicWindow("Login Window")
        window.setHints(listOf(Window.Hint.CENTERED))

        /**
         * Create a composite [Panel] component that can hold multiple sub-components.
         * Something similar to a layout manager.
         */

        val contentPanel = Panel(GridLayout(2))

        /**
         * The [GridLayout] is based on the layout manager with the same name in SWT. In the constructor above we have
         * specified that we want to have a grid with two columns, below we customize the layout further by adding
         * some spacing between the columns.
         */

        val gridLayout: GridLayout = contentPanel.layoutManager as GridLayout
        gridLayout.horizontalSpacing = 3

        /**
         * Below, we use the layout data field attached to each component to
         * give the layout manager extra hints about how it should be placed.
         */

        /**
         * Since the grid has two columns, we can do something like this to add components when we don't need to
         * customize them any further.
         */

        contentPanel.addComponent(Label("Enter Username"))
        val usernameField = TextBox().setLayoutData(GridLayout.createLayoutData(
            GridLayout.Alignment.BEGINNING,
            GridLayout.Alignment.CENTER
        )).setValidationPattern(Pattern.compile("^[A-Za-z0-9_]+"))
        usernameField.addTo(contentPanel)

        /**
         * Some user interactions, like buttons, work by registering callback methods. In this example here, we're
         * using one of the pre-defined dialogs when the button is triggered.
         */

        contentPanel.addComponent(Label(""))
        contentPanel.addComponent(Button("Login") {

            /**
             * Here minimum length 4 means the
             * minimum set length for the username
             */

            if (usernameField.text.length > 4) {

                /**
                 * Here we take entered username,
                 * generated PublicKey and create a new account
                 */

                //TODO: Generate normal PublicKey,
                // not the random one

                val info = UserInfo(
                        Random.nextBytes(32),
                        usernameField.text,
                        System.currentTimeMillis(),
                        true
                )

                val me = Account(info, Random.nextBytes(32), mapOf())
                MessageDialog.showMessageDialog(
                        textGUI,
                        "Hi, ${usernameField.text}",
                        "Welcome to P2Beer Messenger",
                        MessageDialogButton.Continue
                )

                /**
                 * Save generated data
                 */

                val dataStorage = JSONUserDataStorageImpl()
                dataStorage.saveMyData(me)

                val mainW = MainWindow(me)
                mainW.runMainWindow()
                window.close()
            }

        }.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER)))

        /**
         * Close off with an empty row and a separator, then a button to close the window
         */

        contentPanel.addComponent(
            EmptySpace()
                .setLayoutData(
                    GridLayout.createHorizontallyFilledLayoutData(2)
                )
        )
        contentPanel.addComponent(
            Separator(Direction.HORIZONTAL)
                .setLayoutData(
                    GridLayout.createHorizontallyFilledLayoutData(2)
                )
        )
        contentPanel.addComponent(
            Button("Close", window::close).setLayoutData(
                GridLayout.createHorizontallyEndAlignedLayoutData(2)
            )
        )

        /**
         * We now have the content panel fully populated with components. No we attach it to
         * the window.
         */

        window.component = contentPanel

        /**
         * In the "Close" button above, we tied a call to the close() method on the Window object when
         * the button is triggered, this will then break the even loop and our call finally returns.
         */

        textGUI.addWindowAndWait(window)

        /**
         * When our call has returned, the window is closed and no longer visible. The screen still contains the last
         * state the TextGUI left it in, so we can easily add and display another window without any flickering. In
         * this case, we want to shut down the whole thing and return to the ordinary prompt. We just need to stop the
         * underlying Screen for this, the [TextGUI] system does not require any additional disassembly.
         */

    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun tryToParseUserData () : Account {
    val dataStorage = JSONUserDataStorageImpl()
    return dataStorage.loadMyData()
}