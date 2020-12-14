package chat

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import ru.emkn.p2beer.app.client.chat.*
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

fun checkValid(bTree: BTree, node: Node, leftMessage: Message?, rightMessage: Message?): Boolean {
    if (node.isLeaf && node.pointersToChildren.size > 0) {
        return false
    }
    if (!node.isLeaf && node.pointersToMessages.size + 1 != node.pointersToChildren.size) {
        return false
    }

    val messagesInThisNode = getMessages(node, bTree.fileWithMessages)

    if (leftMessage != null && !messagesInThisNode.all { it.compareTo(leftMessage) == 1 }) {
        return false
    }
    if (rightMessage != null && !messagesInThisNode.all { it.compareTo(rightMessage) == -1 }) {
        return false
    }

    var result = true
    for ((index, pointer) in node.pointersToChildren.withIndex()) {
        val child = getNode(bTree.fileWithIndex, pointer, bTree.t)
        val childResult = when(index) {
            0 -> checkValid(bTree, child, null, messagesInThisNode.first())
            node.pointersToChildren.lastIndex -> checkValid(bTree, child, messagesInThisNode.last(), null)
            else -> checkValid(bTree, child, messagesInThisNode[index - 1], messagesInThisNode[index])
        }
        result = result and childResult
    }
    return result
}

class BTreeTests {

    @TestFactory
    fun `check addMessages`(): Stream<DynamicTest> {

        val cases = listOf(10, 20, 100, 500, 1000, 2000)

        return IntStream.range(0, cases.size).mapToObj { n ->
            val bTree = BTree(5,"src/test/kotlin/chat/index.bin", "src/test/kotlin/chat/messages.bin")

            val time = System.currentTimeMillis()

            repeat(cases[n]) {
                addMessage(bTree, createMessage())
            }
            println(System.currentTimeMillis() - time)

            val root = getNode(bTree.fileWithIndex, bTree.pointerToRoot, bTree.t)

            DynamicTest.dynamicTest("Test addMessage with ${cases[n]} messages") {
                assertTrue(checkValid(bTree, root, null, null))
            }
        }
    }

    @TestFactory
    fun `check getKMessages`(): Stream<DynamicTest> {
        val cases = listOf(1, 3, 10, 100, 1000)
        return IntStream.range(0, cases.size).mapToObj { n ->
            val bTree = BTree(5,"src/test/kotlin/chat/index.bin", "src/test/kotlin/chat/messages.bin")

            val messages = mutableListOf<Message>()
            repeat(cases[n] * 2) {
                messages.add(createMessage())
                addMessage(bTree, messages.last())
            }

            messages.sortWith(MessageComparator)
            val position = Random.nextInt(0, cases[n])

            DynamicTest.dynamicTest("Test getKMessage with ${cases[n]} messages") {
                assertEquals(messages.subList(position, position + cases[n]).toList(),
                    getKMessages(bTree, messages[position], cases[n]))
            }
        }
    }
}