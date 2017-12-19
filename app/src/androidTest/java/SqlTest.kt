import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.zeroknowledgeproof.rangeProof.RangeProofTrustedParty
import nl.tudelft.cs4160.identitychain.Util.Key
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock
import nl.tudelft.cs4160.identitychain.database.TrustChainDBHelper
import nl.tudelft.cs4160.identitychain.grpc.asMessage
import nl.tudelft.cs4160.identitychain.grpc.asSetupPublic
import nl.tudelft.cs4160.identitychain.grpc.asZkp
import nl.tudelft.cs4160.identitychain.message.ChainService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SqlTest {
    val trustedParty = RangeProofTrustedParty()
    val zkp = trustedParty.generateProof(30, 18, 100)
    val keyPair = Key.createNewKeyPair()
    val asMessage: ChainService.PublicSetupResult = zkp.first.asMessage()
    val publicPayLoad = asMessage.toByteArray()

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()

        val dbHelper = TrustChainDBHelper(appContext)
        val newBlock = TrustChainBlock.createBlock(publicPayLoad, dbHelper, keyPair.public.encoded, null, keyPair.public.encoded)
        dbHelper.insertInDB(newBlock)

        //get the block out
        println(dbHelper.getBlock(keyPair.public.encoded, newBlock.sequenceNumber).asSetupPublic())
    }

    @Test
    fun lol() {
        val appContext = InstrumentationRegistry.getTargetContext()

        ChainService.PublicSetupResult.parseFrom(publicPayLoad).asZkp()
    }
}