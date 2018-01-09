package nl.tudelft.cs4160.identitychain.grpc

import android.util.Log
import com.google.protobuf.ByteString
import nl.tudelft.cs4160.identitychain.Peer
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock
import nl.tudelft.cs4160.identitychain.block.ValidationResult
import nl.tudelft.cs4160.identitychain.database.TrustChainStorage
import nl.tudelft.cs4160.identitychain.message.ChainService
import nl.tudelft.cs4160.identitychain.message.MessageProto
import java.security.KeyPair
import java.util.*

class BlockSignerValidator(val storage: TrustChainStorage, val keyPair: KeyPair) {
    val myPublicKey = keyPair.public.encoded

    fun signBlock(peer: ChainService.Peer, linkedBlock: MessageProto.TrustChainBlock): MessageProto.TrustChainBlock? {
        // do nothing if linked block is not addressed to me
        if (!Arrays.equals(linkedBlock.linkPublicKey.toByteArray(), myPublicKey)) {
            Log.e(TAG, "signBlock: Linked block not addressed to me.")
            return null
        }
        // do nothing if block is not a request
        if (linkedBlock.linkSequenceNumber != TrustChainBlock.UNKNOWN_SEQ) {
            Log.e(TAG, "signBlock: Block is not a request.")
            return null
        }
        var block = TrustChainBlock.createBlock(null, storage,
                myPublicKey,
                linkedBlock, peer.publicKey.toByteArray())

        block = TrustChainBlock.sign(block, keyPair.private)

        return validateAndSaveBlock(block, storage)

    }


    fun createNewBlock(transaction: ByteArray, peerKey: ByteArray): MessageProto.TrustChainBlock? {
        val newBlock = TrustChainBlock.createBlock(transaction, storage, myPublicKey, null, peerKey)
        val signedBlock = TrustChainBlock.sign(newBlock, keyPair.private)

        return validateAndSaveBlock(signedBlock, storage)
    }

    fun validateAndSaveBlock(block: MessageProto.TrustChainBlock, storgage: TrustChainStorage): MessageProto.TrustChainBlock? {
        val validation: ValidationResult?
        try {
            validation = TrustChainBlock.validate(block, storage)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Block validation failed unexpectedly")
            return null
        }

        Log.i(TAG, "Signed block to " + Peer.bytesToHex(block.linkPublicKey.toByteArray()) +
                ", validation result: " + validation!!.toString())

        // only send block if validated correctly
        // If you want to test the sending of blocks and don't care whether or not blocks are valid, remove the next check.
        if (validation.getStatus() !== ValidationResult.ValidationStatus.PARTIAL_NEXT && validation.getStatus() !== ValidationResult.ValidationStatus.VALID) {
            Log.e(TAG, "Signed block did not validate. Result: " + validation.toString() + ". Errors: "
                    + validation.getErrors().toString())
        } else {
            storage.insertInDB(block)
            return block
        }
        return null
    }

    fun saveCompleteBlock(request: ChainService.PeerTrustChainBlock): ValidationResult? = saveCompleteBlock(request.peer, request.block)

    fun saveCompleteBlock(peer: ChainService.Peer, block: MessageProto.TrustChainBlock): ValidationResult? {

        Log.i(TAG, "Received half block from peer with IP: " + peer.hostname + ":" + peer.port +
                " and public key: " + Peer.bytesToHex(peer.publicKey.toByteArray()))

        if (block.linkPublicKey != null && block.publicKey != null) {

            val validation: ValidationResult
            try {
                validation = TrustChainBlock.validate(block, storage)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

            Log.i(TAG, "Received block validation result " + validation.toString() + "("
                    + TrustChainBlock.toString(block) + ")")

            if (validation.getStatus() === ValidationResult.ValidationStatus.INVALID) {
                for (error in validation.getErrors()) {
                    Log.e(TAG, "Validation error: " + error)
                }
                return null
            } else {
                storage.insertInDB(block)
                Log.i(TAG, "saving complete block")
            }
            return validation
        }

        return null
    }



     fun createPublicKey() =
            ChainService.Key.newBuilder().setPublicKey(ByteString.copyFrom(keyPair.public.encoded)).build()

    companion object {
        val TAG = "BlockSignerValidator"
    }
}