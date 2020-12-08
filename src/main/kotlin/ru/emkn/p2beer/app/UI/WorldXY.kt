package ru.emkn.p2beer.gui

class WorldXY (val sizeX: Int, val sizeY: Int) {

    val tiles = CharArray(sizeX * sizeY)

    fun setTileType (x: Int, y: Int, tile: Char) {
        tiles[x * sizeY + y] = tile
    }

    fun getTileType (x: Int, y: Int): Char {
        return tiles[x * sizeY + y]
    }
}