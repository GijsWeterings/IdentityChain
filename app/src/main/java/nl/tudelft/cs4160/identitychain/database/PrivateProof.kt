package nl.tudelft.cs4160.identitychain.database

import com.zeroknowledgeproof.rangeProof.SetupPrivateResult
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.math.BigInteger

open class PrivateProof(
        @PrimaryKey
        var block_no: Int = 0,
        var m1: ByteArray = ByteArray(0),
        var m2: ByteArray = ByteArray(0),
        var m3: ByteArray = ByteArray(0),
        var r1: ByteArray = ByteArray(0),
        var r2: ByteArray = ByteArray(0),
        var r3: ByteArray = ByteArray(0)
) : RealmObject() {


    fun toPrivateResult() = SetupPrivateResult(BigInteger(m1), BigInteger(m2), BigInteger(m3), BigInteger(r1), BigInteger(r2), BigInteger(r3))

    companion object {
        fun fromPrivateResult(setupPrivateResult: SetupPrivateResult, block_no: Int) =
                with(setupPrivateResult) { PrivateProof(block_no, m1.toByteArray(), m2.toByteArray(), m3.toByteArray(), r1.toByteArray(), r2.toByteArray(), r3.toByteArray()) }
    }
}