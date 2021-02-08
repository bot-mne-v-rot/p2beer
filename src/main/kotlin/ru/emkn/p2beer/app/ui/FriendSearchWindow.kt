package ru.emkn.p2beer.app.ui

import com.googlecode.lanterna.gui2.*
import java.io.IOException

/*
class FriendSearchWindow(private val textGUI: WindowBasedTextGUI) {
    fun build() {
        try {
            val window: Window = BasicWindow("Friend search window")
            window.setHints(listOf(Window.Hint.CENTERED))

            val contentPanel = Panel(GridLayout(2))

            val gridLayout: GridLayout = contentPanel.layoutManager as GridLayout
            gridLayout.horizontalSpacing = 3

            contentPanel.addComponent(Label("Enter user's key"))
            val userKey = TextBox().setLayoutData(
                GridLayout.createLayoutData(
                GridLayout.Alignment.BEGINNING,
                GridLayout.Alignment.CENTER
            ))
            userKey.addTo(contentPanel)

            contentPanel.addComponent(Label(""))

            contentPanel.addComponent(
                Separator(Direction.HORIZONTAL)
                    .setLayoutData(
                        GridLayout.createHorizontallyFilledLayoutData(2)
                    )
            )
            contentPanel.addComponent(
                Button("Add", window::close).setLayoutData(
                    GridLayout.createHorizontallyEndAlignedLayoutData(2)
                )
            )

            window.component = contentPanel

            textGUI.addWindowAndWait(window)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

 */