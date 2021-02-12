package chat

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import ru.emkn.p2beer.app.client.chat.*
import ru.emkn.p2beer.app.client.chat.bTree.BTree
import ru.emkn.p2beer.app.client.chat.bTree.Node
import ru.emkn.p2beer.app.client.chat.bTree.readNode
import java.io.File
import java.util.stream.IntStream
import java.util.stream.Stream
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun getRandomString(length: Int) : String {
    val allowedChars = ('a'..'z')  + ('0'..'9') + '?' + '!' + ',' + '.'
    return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
}

fun createMessage(): Message {
    val text = getRandomString(Random.nextInt(1, 100))
    val uid = Random.nextUInt(0u, UShort.MAX_VALUE + 1u).toUShort()
    val time = Random.nextLong(0, 100000)
    val info = MessageId(Random.nextLong(0, 10000), time, uid)
    val publicKey = Random.nextBytes(32)

    return Message(text, info, publicKey)
}

fun validateBTree(node: Node,
                  leftMessage: Message?,
                  rightMessage: Message?): Boolean {
    if (node.isLeaf && node.pointersToChildren.size > 0)
        return false
    if (!node.isLeaf && node.pointersToMessages.size + 1 != node.pointersToChildren.size)
        return false
    val messages = node.getMessages()
    if ((leftMessage != null && !messages.all { it.compareTo(leftMessage) == 1 }) ||
            (rightMessage != null && !messages.all { it.compareTo(rightMessage) == -1 }))
        return false

    var result = true
    for ((index, pointer) in node.pointersToChildren.withIndex()) {
        val child = node.indexFile.readNode(pointer, node.maxNumberOfChildren, node.indexFile, node.messagesFile)
        val childResult = when(index) {
            0 -> validateBTree(child, null, messages.first())
            node.pointersToChildren.lastIndex -> validateBTree(child, messages.last(), null)
            else -> validateBTree(child, messages[index - 1], messages[index])
        }
        result = result and childResult
    }
    return result
}

class BTreeTests {
    private val pathToIndexFile = "src/test/kotlin/chat/index.bin"
    private val pathToMessagesFile = "src/test/kotlin/chat/messages.bin"

    @BeforeEach
    fun deleteIfExist() {
        if (File(pathToIndexFile).exists())
            File(pathToIndexFile).delete()
        if (File(pathToMessagesFile).exists())
            File(pathToMessagesFile).delete()
    }

    private fun addKMessages(bTree: BTree, k: Int): List<Message> {
        val addedMessages = mutableListOf<Message>()
        repeat(k) {
            addedMessages.add(createMessage())
            bTree.addMessage(addedMessages.last())
        }
        return addedMessages.sortedWith(MessageComparator)
    }

    @TestFactory
    fun `check addMessages`(): Stream<DynamicTest> {
        val cases = listOf(1, 10, 2000)
        return IntStream.range(0, cases.size).mapToObj { n ->
            val bTree = BTree(10,pathToIndexFile, pathToMessagesFile)

            addKMessages(bTree, cases[n])

            DynamicTest.dynamicTest("Test addMessage with ${cases[n]} messages") {
                assertTrue(validateBTree(bTree.root, null, null))
            }
        }
    }

    @TestFactory
    fun `check getKNextMessages`(): Stream<DynamicTest> {
        val cases = listOf(1, 10, 100, 500)
        val bTree = BTree(10, pathToIndexFile, pathToMessagesFile)
        val addedMessages = addKMessages(bTree, 2000)

        return IntStream.range(0, cases.size).mapToObj { n ->
            val position = Random.nextInt(0, cases[n])
            DynamicTest.dynamicTest("Test getKNextMessage with ${cases[n]} messages") {
                assertEquals(addedMessages.subList(position + 1, position + cases[n] + 1).toList(),
                        bTree.getKNextMessages(addedMessages[position], cases[n]))
            }
        }
    }

    @TestFactory
    fun `check getKPreviousMessages`(): Stream<DynamicTest> {
        val cases = listOf(1, 10, 100, 500)
        val bTree = BTree(10, pathToIndexFile, pathToMessagesFile)
        val addedMessages = addKMessages(bTree, 2000)

        return IntStream.range(0, cases.size).mapToObj { n ->
            val position = Random.nextInt(cases[n], addedMessages.size - 1)
            DynamicTest.dynamicTest("Test getKPreviousMessage with ${cases[n]} messages") {
                assertEquals(addedMessages.subList(position - cases[n], position).toList(),
                        bTree.getKPreviousMessages(addedMessages[position], cases[n]))
            }
        }
    }

    @Test
    fun `check getCurrentNumberOfMessages`() {
        val bTree = BTree(10, pathToIndexFile, pathToMessagesFile)
        val number = Random.nextInt(10, 1000)
        addKMessages(bTree, number)
        assertEquals(number, bTree.getCurrentNumberOfMessages())
    }

    @Test
    fun `check getLastMessage`() {
        val bTree = BTree(10, pathToIndexFile, pathToMessagesFile)
        val addedMessages = addKMessages(bTree, Random.nextInt(100, 1000))
        assertEquals(addedMessages.last(), bTree.getLastMessage())
    }

    @TestFactory
    fun `check getKLastMessages`(): Stream<DynamicTest> {
        val cases = listOf(1, 10, 100, 500)
        val bTree = BTree(10, pathToIndexFile, pathToMessagesFile)
        val addedMessages = addKMessages(bTree, 2000)

        return IntStream.range(0, cases.size).mapToObj { n ->
            val position = addedMessages.size - 1
            DynamicTest.dynamicTest("Test getKLastMessage with ${cases[n]} messages") {
                assertEquals(addedMessages.subList(position - cases[n] + 1, position + 1).toList(),
                        bTree.getKLastMessages(cases[n]))
            }
        }
    }
}
