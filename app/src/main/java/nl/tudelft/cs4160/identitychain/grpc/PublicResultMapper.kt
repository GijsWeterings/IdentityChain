package nl.tudelft.cs4160.identitychain.grpc

import com.google.protobuf.ByteString
import com.zeroknowledgeproof.rangeProof.Challenge
import com.zeroknowledgeproof.rangeProof.CommittedIntegerProof
import com.zeroknowledgeproof.rangeProof.InteractivePublicResult
import com.zeroknowledgeproof.rangeProof.SetupPublicResult
import nl.tudelft.cs4160.identitychain.message.ChainService
import java.math.BigInteger

fun SetupPublicResult.asMessage(): ChainService.PublicSetupResult {
    return ChainService.PublicSetupResult.newBuilder()
            .setC(this.c.asByteString())
            .setC1(this.c1.asByteString())
            .setC2(this.c2.asByteString())
            .setCPrime(this.cPrime.asByteString())
            .setCDPrime(this.cDPrime.asByteString())
            .setC1Prime(this.c1Prime.asByteString())
            .setC2Prime(this.c2Prime.asByteString())
            .setC3Prime(this.c3Prime.asByteString())
            .setG(this.g.asByteString())
            .setH(this.h.asByteString())
            .setK1(this.k1.asByteString())
            .setN(this.N.asByteString())
            .setSameCommitment(this.sameCommitment.asMessage())
            .setCdPrimeIsSquare(this.cDPrimeIsSquare.asMessage())
            .setM3IsSquare(this.m3IsSquare.asMessage())
            .setA(this.a)
            .setB(this.b)
            .build()
}

fun ChainService.PublicSetupResult.asZkp(): SetupPublicResult {
    return SetupPublicResult(this.c.asBigInt(), this.c1.asBigInt(), this.c2.asBigInt(),
            this.sameCommitment.asZkp(), this.cPrime.asBigInt(), this.cdPrime.asBigInt(),
            this.cdPrimeIsSquare.asZkp(), this.c1Prime.asBigInt(), this.c2Prime.asBigInt(), this.c3Prime.asBigInt(),
            this.m3IsSquare.asZkp(), this.g.asBigInt(), this.h.asBigInt(), this.k1.asBigInt(), this.n.asBigInt(), this.a, this.b)
}

fun CommittedIntegerProof.asMessage(): ChainService.CommittedIntegerProof {
    return ChainService.CommittedIntegerProof.newBuilder()
            .setG1(this.g1.asByteString())
            .setG2(this.g2.asByteString())
            .setH1(this.h1.asByteString())
            .setH2(this.h2.asByteString())
            .setE(this.E.asByteString())
            .setF(this.F.asByteString())
            .setC(this.c.asByteString())
            .setD(this.D.asByteString())
            .setD1(this.D1.asByteString())
            .setD2(this.D2.asByteString())
            .build()
}

fun ChainService.CommittedIntegerProof.asZkp(): CommittedIntegerProof {
    return CommittedIntegerProof(this.g1.asBigInt(), this.g2.asBigInt(), this.h1.asBigInt(),
            this.h2.asBigInt(), this.e.asBigInt(), this.f.asBigInt(), this.c.asBigInt(), this.d.asBigInt(),
            this.d1.asBigInt(), this.d2.asBigInt())
}

fun ChainService.ChallengeReply.asZkp(): (BigInteger, BigInteger) -> InteractivePublicResult = { s, t ->
    InteractivePublicResult(this.x.asBigInt(), this.y.asBigInt(), this.u.asBigInt(), this.v.asBigInt(), Challenge(s, t))
}

fun InteractivePublicResult.asChallengeReply() = ChainService.ChallengeReply.newBuilder()
        .setX(this.x.asByteString())
        .setY(this.y.asByteString())
        .setU(this.u.asByteString())
        .setV(this.v.asByteString())
        .build()

fun BigInteger.asByteString(): ByteString = ByteString.copyFrom(this.toByteArray())

fun ByteString.asBigInt() = BigInteger(this.toByteArray())