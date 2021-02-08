package chat

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import ru.emkn.p2beer.app.client.chat.*
import java.io.File
import java.io.RandomAccessFile
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

fun validateBTree(bTree: BTree,
                  node: Node,
                  leftMessage: Message?,
                  rightMessage: Message?): Boolean {
    if (node.isLeaf() && node.pointersToChildren.size > 0)
        return false
    if (!node.isLeaf() && node.pointersToMessages.size + 1 != node.pointersToChildren.size)
        return false
    val messages = node.getMessages(bTree.messagesFile)
    if ((leftMessage != null && !messages.all { it.compareTo(leftMessage) == 1 }) ||
            (rightMessage != null && !messages.all { it.compareTo(rightMessage) == -1 }))
        return false

    var result = true
    for ((index, pointer) in node.pointersToChildren.withIndex()) {
        val child = bTree.indexFile.readNode(pointer, bTree.maxNumberOfChildren)
        val childResult = when(index) {
            0 -> validateBTree(bTree, child, null, messages.first())
            node.pointersToChildren.lastIndex -> validateBTree(bTree, child, messages.last(), null)
            else -> validateBTree(bTree, child, messages[index - 1], messages[index])
        }
        result = result and childResult
    }
    return result
}

/*
fun printBTree(bTree: BTree, node: Node) {
    println("We are in node ${node.positionInIndexFile}")
    print("Messages: ")
    val messages = node.getMessages(bTree.messagesFile)
    for (message in messages)
        print("${message.info.timestamp} ")
    print("\nChildren: ")
    for (pointer in node.pointersToChildren)
        print("$pointer ")
    print("\n\n")
    for (pointer in node.pointersToChildren)
        printBTree(bTree, bTree.indexFile.readNode(pointer, bTree.maxNumberOfChildren))
}*/


class BTreeTests {
    @TestFactory
    fun `check addMessages`(): Stream<DynamicTest> {
        if (File("src/test/kotlin/chat/index.bin").exists()) {
            File("src/test/kotlin/chat/index.bin").delete()
            File("src/test/kotlin/chat/messages.bin").delete()
        }

        val cases = listOf(20)//, 20, 100, 500, 1000, 2000)
        return IntStream.range(0, cases.size).mapToObj { n ->
            val bTree = BTree(10,"src/test/kotlin/chat/index.bin", "src/test/kotlin/chat/messages.bin")

            repeat(cases[n]) {
                bTree.addMessage(createMessage())
            }

            //printBTree(bTree, bTree.root)

            DynamicTest.dynamicTest("Test addMessage with ${cases[n]} messages") {
                assertTrue(validateBTree(bTree, bTree.root, null, null))
            }
        }
    }

    /*
    @TestFactory
    fun `check addMessages`(): Stream<DynamicTest> {
        if (File("src/test/kotlin/chat/index.bin").exists()) {
            File("src/test/kotlin/chat/index.bin").delete()
            File("src/test/kotlin/chat/messages.bin").delete()
        }

        val cases = listOf(10, 20, 100, 500, 1000, 2000)

        return IntStream.range(0, cases.size).mapToObj { n ->
            val bTree = BTree(5,"src/test/kotlin/chat/index.bin", "src/test/kotlin/chat/messages.bin")

            repeat(cases[n]) {
                addMessage(bTree, createMessage())
            }

            val root = getRoot(bTree)

            DynamicTest.dynamicTest("Test addMessage with ${cases[n]} messages") {
                assertTrue(checkValid(bTree, root, null, null))
            }
        }
    }
*/
    /*
    @TestFactory
    fun `check getKNextMessages`(): Stream<DynamicTest> {
        if (File("src/test/kotlin/chat/index.bin").exists()) {
            File("src/test/kotlin/chat/index.bin").delete()
            File("src/test/kotlin/chat/messages.bin").delete()
        }

        val cases = listOf(1, 10 , 100, 500)

        val bTree = BTree(5,"src/test/kotlin/chat/index.bin", "src/test/kotlin/chat/messages.bin")

        val addedMessages = mutableListOf<Message>()
        repeat(2000) {
            addedMessages.add(createMessage())
            addMessage(bTree, addedMessages.last())
        }
        addedMessages.sortWith(MessageComparator)

        return IntStream.range(0, cases.size).mapToObj { n ->
            val position = Random.nextInt(0, cases[n])
            DynamicTest.dynamicTest("Test getKNextMessage with ${cases[n]} messages") {
                assertEquals(addedMessages.subList(position + 1, position + cases[n] + 1).toList(),
                        getKNextMessages(bTree, addedMessages[position], cases[n]))
            }
        }
    }

    @TestFactory
    fun `check getKPreviousMessages`(): Stream<DynamicTest> {
        if (File("src/test/kotlin/chat/index.bin").exists()) {
            File("src/test/kotlin/chat/index.bin").delete()
            File("src/test/kotlin/chat/messages.bin").delete()
        }

        val cases = listOf(1, 10, 100, 500)

        val bTree = BTree(5,"src/test/kotlin/chat/index.bin", "src/test/kotlin/chat/messages.bin")

        val addedMessages = mutableListOf<Message>()
        repeat(2000) {
            addedMessages.add(createMessage())
            addMessage(bTree, addedMessages.last())
        }
        addedMessages.sortWith(MessageComparator)

        return IntStream.range(0, cases.size).mapToObj { n ->
            val position = Random.nextInt(cases[n], addedMessages.size - 1)
            DynamicTest.dynamicTest("Test getKPreviousMessage with ${cases[n]} messages") {
                assertEquals(addedMessages.subList(position - cases[n], position).toList().reversed(),
                        getKPreviousMessages(bTree, addedMessages[position], cases[n]))
            }
        }
    }

    @Test
    fun `check getNumberOfMessages`() {
        if (File("src/test/kotlin/chat/index.bin").exists()) {
            File("src/test/kotlin/chat/index.bin").delete()
            File("src/test/kotlin/chat/messages.bin").delete()
        }

        val bTree = BTree(5,"src/test/kotlin/chat/index.bin", "src/test/kotlin/chat/messages.bin")

        val number = Random.nextInt(10, 1000)
        repeat(number) {
            addMessage(bTree, createMessage())
        }

        assertEquals(number, getNumberOfMessages(bTree))
    }

    @Test
    fun `check getLastMessage`() {
        if (File("src/test/kotlin/chat/index.bin").exists()) {
            File("src/test/kotlin/chat/index.bin").delete()
            File("src/test/kotlin/chat/messages.bin").delete()
        }

        val bTree = BTree(5,"src/test/kotlin/chat/index.bin", "src/test/kotlin/chat/messages.bin")

        val addedMessages = mutableListOf<Message>()
        repeat(Random.nextInt(100, 1000)) {
            addedMessages.add(createMessage())
            addMessage(bTree, addedMessages.last())
        }

        assertEquals(addedMessages.maxWithOrNull(MessageComparator), getLastMessage(bTree))
    }

    @TestFactory
    fun `check getKLastMessages`(): Stream<DynamicTest> {
        if (File("src/test/kotlin/chat/index.bin").exists()) {
            File("src/test/kotlin/chat/index.bin").delete()
            File("src/test/kotlin/chat/messages.bin").delete()
        }

        val cases = listOf(1, 10, 100, 1000)

        val bTree = BTree(5,"src/test/kotlin/chat/index.bin", "src/test/kotlin/chat/messages.bin")

        val addedMessages = mutableListOf<Message>()
        repeat(2000) {
            addedMessages.add(createMessage())
            addMessage(bTree, addedMessages.last())
        }
        addedMessages.sortWith(MessageComparator)

        return IntStream.range(0, cases.size).mapToObj { n ->
            val position = addedMessages.size - 1
            DynamicTest.dynamicTest("Test getKLastMessage with ${cases[n]} messages") {
                assertEquals(addedMessages.subList(position - cases[n] + 1, position + 1).toList().reversed(),
                        getKLastMessages(bTree, cases[n]))
            }
        }
    }*/
}