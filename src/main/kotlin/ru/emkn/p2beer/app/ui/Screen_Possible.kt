package ru.emkn.p2beer.gui

import com.google.common.collect.ArrayTable
import com.google.common.collect.Table
import com.google.common.collect.Tables
import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import com.googlecode.lanterna.terminal.TerminalResizeListener
import kotlin.system.exitProcess


sealed class Tile(val char: Char, val color: TextColor.ANSI) {
    class Floor() : Tile('\u00B7', TextColor.ANSI.CYAN)
    class Wall() : Tile('\u2593', TextColor.ANSI.WHITE)
    class Bounds() : Tile('x', TextColor.ANSI.BLACK)
}

class WorldBuilder(private val height: Int, private val width: Int) {

    var worldTiles: Table<Int, Int, Tile> =
        randomWorld(ArrayTable.create<Int, Int, Tile>(0 until height, 0 until width))

    private val smoothing = 1..8

    private val offset = -1..1

    fun build(): WorldView {
        return WorldView(smooth(worldTiles))
    }

    private fun randomWorld(tiles: Table<Int, Int, Tile>): Table<Int, Int, Tile> {
        return Tables.transformValues(tiles, { _ -> if (Math.random() < 0.5) Tile.Floor() else Tile.Wall() })
    }

    fun smooth(tiles: Table<Int, Int, Tile>): Table<Int, Int, Tile> {

        var result = tiles

        val tiles2 = ArrayTable.create<Int, Int, Tile>(0 until height, 0 until width)

        smoothing.forEach {
            for (tileCell in result.cellSet()) {
                val col = tileCell.columnKey
                val row = tileCell.rowKey
                var floors = 0
                var walls = 0
                offset.forEach { ox ->
                    (offset.forEach { oy ->
                        when (result.get(row?.plus(oy), col?.plus(ox))) {
                            is Tile.Floor -> floors = floors.inc()
                            is Tile.Wall -> walls = walls.inc()
                        }
                    })
                }
                tiles2.put(
                    tileCell.rowKey, tileCell.columnKey,
                    if (floors >= walls) Tile.Floor() else Tile.Wall()
                )
            }
            result = tiles2
        }
        return result
    }
}

class WorldView(val tiles: Table<Int, Int, Tile>) {
}

class DefaultWriter(val screen: Screen) {

    fun centerText(text: String, vararg modifiers: SGR) {
        val x = (screen.terminalSize.columns - text.length) / 2
        val y = screen.terminalSize.rows / 2
        val graphics = screen.newTextGraphics()
        graphics.enableModifiers(*modifiers);
        graphics.putString(x, y, text)
    }

    fun drawText(column: Int, row: Int, text: String, vararg modifiers: SGR) {
        TODO()
    }
}

class WorldWriter(private val screen: Screen) {

    fun drawWorld() {

        val text = screen.newTextGraphics()

        val world = WorldBuilder(screen.terminalSize.rows, screen.terminalSize.columns).build()

        val tiles = world.tiles.cellSet()

        for (tile in tiles) {
            text.setCharacter(
                tile.columnKey!!, tile.rowKey!!,
                TextCharacter(tile.value!!.char, tile.value!!.color, TextColor.ANSI.DEFAULT)
            )
        }
    }
}

fun main(args: Array<String>) {
    val term = DefaultTerminalFactory().createTerminal()
    val screen = TerminalScreen(term)
    val writerDefault = DefaultWriter(screen)
    val worldWriter = WorldWriter(screen)

    fun updateWorld() {
        screen.clear()
        worldWriter.drawWorld()
        draw(writerDefault)
        screen.refresh()
    }

    val resized = TerminalResizeListener { _, _ ->
        updateWorld()
    }

    term.addResizeListener(resized)

    screen.startScreen()

    updateWorld()

    while (!(term.readInput().character == 'x' && term.pollInput().isCtrlDown)) {
        Thread.sleep(25)
    }

    screen.stopScreen()

    exitProcess(0)
}

fun draw(writer: DefaultWriter) {
    writer.centerText("Welcome to P2Beer Messenger!", SGR.BLINK)
    //writer.drawText(0, writer.screen.terminalSize.rows - 1, "griffio 2017", SGR.UNDERLINE)
}