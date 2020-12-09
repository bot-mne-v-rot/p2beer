package ru.emkn.p2beer.app.ui

import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import java.awt.SystemColor
import java.io.IOException


fun main(args: Array<String>) {
    try {

        val screen = DefaultTerminalFactory().createScreen()
        screen.startScreen()

        val textGUI: WindowBasedTextGUI = MultiWindowTextGUI(screen, TextColor.ANSI.CYAN)

        /**
         * Creating a new window
         */
        val window: Window = BasicWindow("Dialog Window")
        window.setHints(listOf(Window.Hint.FULL_SCREEN));

        val mainPanel = Panel()
        mainPanel.layoutManager = LinearLayout(Direction.HORIZONTAL)

        val leftPanel = Panel()

        mainPanel.addComponent(leftPanel.withBorder(Borders.singleLine("Left Panel")))

        val rightPanel = Panel()
        mainPanel.addComponent(rightPanel.withBorder(Borders.singleLine("Right Panel")))

        mainPanel.addComponent(
            Button("Close", window::close).setLayoutData(
                GridLayout.createHorizontallyEndAlignedLayoutData(2)
            )
        )

        window.component = mainPanel.withBorder(Borders.singleLine("Main Panel"))

        textGUI.addWindowAndWait(window)

    } catch (e: IOException) {
        e.printStackTrace()
    }
}