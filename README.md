
# 
# IdentityChain

Students:

- @GijsWeterings
- @LiamClark
- @Schubbcasten
- @Recognition2
- @eanker

## Self-Sovereign Identity

IdentityChain is a Self-Sovereign Identity app based on [TrustChain](https://www.sciencedirect.com/science/article/pii/S0167739X17318988). It aims to facilitate a system where identity can be decentralized, and give control of the use of identity data back to the user. Identity in this case is built up from all attestations made between the user and other parties. Each block on the TrustChain is signed by the user and a second party, and contains a zero knowledge proof.

## Zero knowledge proof

In the IdentityChain app zero knowledge range proofs can be generated, proven and verified.
The implementation is build upon the paper “[An Efficient Range Proof Scheme](http://ai2-s2-pdfs.s3.amazonaws.com/6bdb/0c85de3b38113c30c99b63a9fb48190af73e.pdf)” by Kun Peng and Feng Bao. This means that we can proof a number is in a certain range, without disclosing the number itself. For example, you can proof that you are older then 18 whilst buying alcohol, without telling your real age. 


## Network discovery and communication

The IdentityChain app is designed for local, face-to-face interactions. To facilitate the process of connecting between devices, IdentityChain makes use of [*Network Service Discovery*](https://developer.android.com/reference/android/net/nsd/NsdManager.html) **(NSD). 

On startup, the app announces itself to the network. Other devices running the app on the same network can now discover the peer device, and connect directly via local IP.

Once a peer connection is made, a gRPC channel is set up between the devices, which is encrypted to ensure safe communication.

## Attestations

A peer can request attestation for a certain claim from another peer. He will send a half-block containing a zero-knowledge proof. This creates an attestation request on the side of the attestee. This request can be accepted or rejected and when accepted the zero knowledge proof gets verified and the block gets then signed and sent back to the requester.

## Verifications

The IdentityChain app can also verify blocks created by other people. 
Verification is an interactive protocol between the attestee and the verifier.
This protocol checks all parameters of the zero-knowledge proof.



## Screenshots
<a href="https://d2mxuefqeaa7sj.cloudfront.net/s_FA4A66FA020261CFD77B30A0A2A8A0ED2DCFDCCA43BE7FE0E43C51BDC0108A30_1516112314899_photo_2018-01-16_15-18-24.jpg"><img src="https://d2mxuefqeaa7sj.cloudfront.net/s_FA4A66FA020261CFD77B30A0A2A8A0ED2DCFDCCA43BE7FE0E43C51BDC0108A30_1516112314899_photo_2018-01-16_15-18-24.jpg" align="left" height="300"></a>
<a href="https://d2mxuefqeaa7sj.cloudfront.net/s_FA4A66FA020261CFD77B30A0A2A8A0ED2DCFDCCA43BE7FE0E43C51BDC0108A30_1516112314924_photo_2018-01-16_15-18-16.jpg"><img src="https://d2mxuefqeaa7sj.cloudfront.net/s_FA4A66FA020261CFD77B30A0A2A8A0ED2DCFDCCA43BE7FE0E43C51BDC0108A30_1516112314924_photo_2018-01-16_15-18-16.jpg" align="left" height="300"></a>
<a href="https://d2mxuefqeaa7sj.cloudfront.net/s_FA4A66FA020261CFD77B30A0A2A8A0ED2DCFDCCA43BE7FE0E43C51BDC0108A30_1516112314887_photo_2018-01-16_15-18-12.jpg"><img src="https://d2mxuefqeaa7sj.cloudfront.net/s_FA4A66FA020261CFD77B30A0A2A8A0ED2DCFDCCA43BE7FE0E43C51BDC0108A30_1516112314887_photo_2018-01-16_15-18-12.jpg" align="left" height="300"></a>
<a href="https://d2mxuefqeaa7sj.cloudfront.net/s_C50C09168F44C67D47C02E9002CB691AB9FCFB08351883DCAC77195AEA8F3704_1517603298296_new-requests.png"><img src="https://d2mxuefqeaa7sj.cloudfront.net/s_C50C09168F44C67D47C02E9002CB691AB9FCFB08351883DCAC77195AEA8F3704_1517603298296_new-requests.png" align="left" height="300"></a>
<a href="https://d2mxuefqeaa7sj.cloudfront.net/s_D23F1922FC49974EDFBD88A6346CDFAE74E84C1DE46F49ADA4BEFEA008B1107D_1517592721914_photo_2018-02-02_18-31-36.jpg"><img src="https://d2mxuefqeaa7sj.cloudfront.net/s_D23F1922FC49974EDFBD88A6346CDFAE74E84C1DE46F49ADA4BEFEA008B1107D_1517592721914_photo_2018-02-02_18-31-36.jpg" align="left" height="300"></a>
<a href="https://d2mxuefqeaa7sj.cloudfront.net/s_C50C09168F44C67D47C02E9002CB691AB9FCFB08351883DCAC77195AEA8F3704_1517596707116_peer-view.png"><img src="https://d2mxuefqeaa7sj.cloudfront.net/s_C50C09168F44C67D47C02E9002CB691AB9FCFB08351883DCAC77195AEA8F3704_1517596707116_peer-view.png" align="left" height="300"></a>
<a href="https://d2mxuefqeaa7sj.cloudfront.net/s_C50C09168F44C67D47C02E9002CB691AB9FCFB08351883DCAC77195AEA8F3704_1517596731835_verify.png"><img src="https://d2mxuefqeaa7sj.cloudfront.net/s_C50C09168F44C67D47C02E9002CB691AB9FCFB08351883DCAC77195AEA8F3704_1517596731835_verify.png" align="left" height="300"></a>


