package nl.tudelft.cs4160.identitychain.database

import com.google.protobuf.ByteString
import nl.tudelft.cs4160.identitychain.message.MessageProto

class TrustChainMemoryStorage : TrustChainStorage {
    val blocks: MutableList<MessageProto.TrustChainBlock> = ArrayList()
    override fun insertInDB(block: MessageProto.TrustChainBlock): Long {
        blocks.add(block)
        return 0L
    }

    override fun getBlock(pubkey: ByteArray, seqNumber: Int): MessageProto.TrustChainBlock = blocks.find((hasPubKey(pubkey) and hasSequenceNumber(seqNumber)))!!


    override fun getLinkedBlock(block: MessageProto.TrustChainBlock): MessageProto.TrustChainBlock {
        val where = hasPubKey(block.linkPublicKey) and hasSequenceNumber(block.linkSequenceNumber) and hasLinkPubKey(block.publicKey) and hasLinkSequenceNumber(block.sequenceNumber)
        return blocks.find(where)!!
    }

    override fun getBlockBefore(pubkey: ByteArray, seqNumber: Int): MessageProto.TrustChainBlock {
        return blocks.filter(hasPubKey(pubkey) and comesBefore(seqNumber)).maxBy { it.sequenceNumber }!!
    }

    override fun getBlockAfter(pubkey: ByteArray, seqNumber: Int): MessageProto.TrustChainBlock {
        return blocks.filter(hasPubKey(pubkey) and comesAfter(seqNumber)).minBy { it.sequenceNumber }!!
    }

    override fun getLatestBlock(pubkey: ByteArray): MessageProto.TrustChainBlock = getBlock(pubkey, getMaxSeqNum(pubkey))

    override fun getMaxSeqNum(pubkey: ByteArray): Int = blocks.filter(hasPubKey(pubkey)).map { it.sequenceNumber }.max() ?: -1

    override fun getAllBlocks(): MutableList<MessageProto.TrustChainBlock> = blocks

    override fun crawl(pubKey: ByteArray, seqNum: Int, limit: Int): MutableList<MessageProto.TrustChainBlock> = blocks.filter(hasPubKey(pubKey) and comesAfter(seqNum)).subList(0, 100).toMutableList()

    override fun crawl(pubKey: ByteArray, seqNum: Int): MutableList<MessageProto.TrustChainBlock> = crawl(pubKey, seqNum, 100)

    infix inline fun <T> ((T) -> Boolean).and(crossinline b: (T) -> Boolean): (T) -> Boolean = { this(it) && b(it) }

    fun hasPubKey(pubkey: ByteArray): (MessageProto.TrustChainBlock) -> Boolean = { it.publicKey.toByteArray().contentEquals(pubkey) }
    fun hasPubKey(pubkey: ByteString): (MessageProto.TrustChainBlock) -> Boolean = hasPubKey(pubkey.toByteArray())
    fun hasLinkPubKey(pubkey: ByteString): (MessageProto.TrustChainBlock) -> Boolean = { it.linkPublicKey == pubkey }

    fun hasSequenceNumber(seqNumber: Int): (MessageProto.TrustChainBlock) -> Boolean = { it.sequenceNumber == seqNumber }
    fun hasLinkSequenceNumber(seqNumber: Int): (MessageProto.TrustChainBlock) -> Boolean = { it.linkSequenceNumber == seqNumber }

    fun comesBefore(seqNumber: Int): (MessageProto.TrustChainBlock) -> Boolean = { it.sequenceNumber < seqNumber }
    fun comesAfter(seqNumber: Int): (MessageProto.TrustChainBlock) -> Boolean = { it.sequenceNumber > seqNumber }
}
