package nl.tudelft.cs4160.identitychain.attestation

import io.reactivex.Single

class AttestationRequest()

class AttestationMetaData(val name: String, val expirationDate: String, val proof: ZeroKnowledgeProof)

sealed class ZeroKnowledgeProof

class RangeProof() : ZeroKnowledgeProof()

//sort of saved this as a block super cool
fun attestation(): Single<ZeroKnowledgeProof> = TODO("make this ")

fun prover(): Single<Boolean> {
    val a = receive<Pair<Int, Int>>()
    return a.map { (t,s) -> respondToChallenge(t, s) }
            .flatMap { send<ChallengeResponse, Boolean>(it) }

}

fun verifier(): Single<Boolean> {
    val challenge = Pair(20, 50)
    val response: Single<ChallengeResponse> = send<Pair<Int, Int>, ChallengeResponse>(challenge)
    return response.map(ChallengeResponse::verify)
}

fun generateResponse(t: Int, s: Int) {

}

fun respondToChallenge(t: Int, s: Int): ChallengeResponse = ChallengeResponse(0, 0, 0, 0)


fun <A, B> send(a: A): Single<B> = TODO("")

fun <A> receive(): Single<A> = TODO("")

data class ChallengeResponse(val x: Int, val y: Int, val u: Int, val v: Int) {
    fun verify(): Boolean = false
}


