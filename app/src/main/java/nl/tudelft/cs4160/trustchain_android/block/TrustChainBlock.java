package nl.tudelft.cs4160.trustchain_android.block;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;
import android.util.Log;

import com.google.protobuf.ByteString;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nl.tudelft.cs4160.trustchain_android.Util.Key;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBContract;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

/**
 * Created by meijer on 20-9-17.
 */

public class TrustChainBlock {
    public static final ByteString GENESIS_HASH = ByteString.copyFrom(new byte[] {0x00});
    public static final int GENESIS_SEQ = 1;
    public static final int UNKNOWN_SEQ = 0;
    public static final ByteString EMPTY_SIG = ByteString.copyFrom(new byte[] {0x00});
    public static final ByteString EMPTY_PK = ByteString.copyFrom(new byte[] {0x00});

    // TODO: remove
    public static final ByteString TEMP_PEER_PK = ByteString.copyFrom(new byte[] {0x01});

    /**
     * Creates a TrustChain genesis block using protocol buffers.
     * @return block - A MessageProto.TrustChainBlock
     */
    public static MessageProto.TrustChainBlock createGenesisBlock(KeyPair kp) {
        MessageProto.TrustChainBlock block = MessageProto.TrustChainBlock.newBuilder()
                .setTransaction(ByteString.EMPTY)
                .setPublicKey(ByteString.copyFrom(kp.getPublic().getEncoded()))
                .setSequenceNumber(GENESIS_SEQ)
                .setLinkPublicKey(EMPTY_PK)
                .setLinkSequenceNumber(UNKNOWN_SEQ)
                .setPreviousHash(GENESIS_HASH)
                .setSignature(EMPTY_SIG)
                .build();
        block = sign(block, kp.getPrivate());
        return block;
    }

    /**
     * Creates a TrustChainBlock for the given input.
     * @param transaction - Details the message of the block, can be null if there is a linked block
     * @param db - database in which the previous blocks of this peer can be found
     * @param mypubk - the public key of this peer
     * @param linkedBlock - The halfblock that is linked to this to be created half block, can be null
     * @param linkpubk - The public key of the linked peer
     * @return a new half block
     */
    public static MessageProto.TrustChainBlock createBlock(byte[] transaction, SQLiteDatabase db,
                                                         byte[] mypubk, MessageProto.TrustChainBlock linkedBlock,
                                                         byte[] linkpubk) {
        MessageProto.TrustChainBlock latestBlock = getBlock(db,mypubk,getMaxSeqNum(db,mypubk));

        MessageProto.TrustChainBlock.Builder builder = MessageProto.TrustChainBlock.newBuilder();
        if(linkedBlock != null) {
            builder.setTransaction(linkedBlock.getTransaction())
                    .setLinkPublicKey(linkedBlock.getPublicKey())
                    .setLinkSequenceNumber(linkedBlock.getSequenceNumber());
        } else {
            builder.setTransaction(ByteString.copyFrom(transaction))
                    .setLinkPublicKey(ByteString.copyFrom(linkpubk))
                    .setLinkSequenceNumber(TrustChainBlock.UNKNOWN_SEQ);
        }

        // if a latest block was found set the correct fields, if not the block won't be validated
        // so it doesn't matter much that no exception is raised here.
        if(latestBlock != null) {
            builder.setSequenceNumber(latestBlock.getSequenceNumber() + 1)
                    .setPreviousHash(ByteString.copyFrom(hash(latestBlock)));
        }

        builder.setPublicKey(ByteString.copyFrom(mypubk));
        builder.setSignature(EMPTY_SIG);

        return builder.build();
    }

    /**
     * Checks if the given block is a genesis block
     * @param block - TrustChainBlock that we want to check
     * @return boolean - true if the block is a genesis block, false otherwise
     */
    public static boolean isGenesisBlock(MessageProto.TrustChainBlock block) {
        return (block.getSequenceNumber() == GENESIS_SEQ) || (block.getPreviousHash() == GENESIS_HASH);
    }

    /**
     * Returns a sha256 hash of the block.
     * @param block - a proto Trustchain block
     * @return the sha256 hash of the byte array of the block
     */
    public static byte[] hash(MessageProto.TrustChainBlock block) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        //remove the signature (if there is any)
        MessageProto.TrustChainBlock rawBlock = block.toBuilder().setSignature(EMPTY_SIG).build();
        return md.digest(rawBlock.toByteArray());
    }


    /**
     * Signs this block with a given public key.
     */
    public static MessageProto.TrustChainBlock sign(MessageProto.TrustChainBlock block, PrivateKey privateKey) {
        //sign the hash
        byte[] hash = TrustChainBlock.hash(block);
        byte[] signature = Key.sign(privateKey, hash);

        //create the block
        return block.toBuilder().setSignature(ByteString.copyFrom(signature)).build();
    }

    /**
     * Validates this block against what is known in the database in 6 steps.
     * Returns the validation result and errors. Any error will result in a false validation.
     * @param block - block that needs to be validated
     * @param dbHelper - dbHelper which contains the db to check against
     * @return a validation result, containing the actual validation result and a list of errors
     */
    public static ValidationResult validate(MessageProto.TrustChainBlock block, TrustChainDBHelper dbHelper) throws Exception {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ValidationResult result = new ValidationResult();
        List<String> errors = new ArrayList<>();

        // ** Step 1: Get all the related blocks from the database **
        // The validity of blocks is immutable. Once they are accepted they cannot change validation
        // result. In such cases subsequent blocks can get validation errors and will not get
        // inserted into the database. Thus we can assume that all retrieved blocks are all valid
        // themselves. Blocks can get inserted into the database in any order, so we need to find
        // successors, predecessors as well as the block itself and its linked block.
        MessageProto.TrustChainBlock dbBlock = getBlock(db,block.getPublicKey().toByteArray(),block.getSequenceNumber());
        MessageProto.TrustChainBlock linkBlock = getBlock(db,block.getLinkPublicKey().toByteArray(),block.getLinkSequenceNumber());
        MessageProto.TrustChainBlock prevBlock = getBlockBefore(db,block.getPublicKey().toByteArray(),block.getSequenceNumber()-1);
        MessageProto.TrustChainBlock nextBlock = getBlockAfter(db,block.getPublicKey().toByteArray(),block.getSequenceNumber()+1);

        // ** Step 2: Determine the maximum validation level **
        // Depending on the blocks we get from the database, we can decide to reduce the validation
        // level. We must do this prior to flagging any errors. This way we are only ever reducing
        // the validation level without having to resort to min()/max() every time we set it.
        if(prevBlock == null && nextBlock == null) {
            // If it is not a genesis block we know nothing about this public key, else pretend prevblock exists
            if(!isGenesisBlock(block)) {
                result.setNoInfo();
            } else {
                result.setPartialNext();
            }
        } else if(prevBlock == null) {
            // If it is not a genesis block we are missing the previous block
            if(!isGenesisBlock(block)){
                result.setPartialPrevious();
                // If there is a gap between this block and the next we have a full partial validation result
                if(nextBlock.getSequenceNumber() != block.getSequenceNumber() + 1){
                    result.setPartial();
                }
            }
            // if it is a genesis block, ignore that there is no previous block, check for a gap for the next block
            else if(nextBlock.getSequenceNumber() != block.getSequenceNumber() + 1) {
                result.setPartialNext();
            }
        } else if(nextBlock == null) {
            // The next block is missing so partial next at best
            result.setPartialNext();
            // If there is a gap between this and the previous block, full partial validation result
            if(prevBlock.getSequenceNumber() != block.getSequenceNumber() - 1) {
                result.setPartial();
            }
        } else {
            // Both sides have known blocks, check for gaps
            // check gap previous
            if(prevBlock.getSequenceNumber() != block.getSequenceNumber() - 1) {
                result.setPartialPrevious();
                // check gap previous and next
                if(nextBlock.getSequenceNumber() != block.getSequenceNumber() + 1){
                    result.setPartial();
                }
            } else {
                // check gap next block, if not the result stays valid
                if(nextBlock.getSequenceNumber() != block.getSequenceNumber() + 1){
                    result.setPartialNext();
                }
            }
        }

        // ** Step 3: validate that the block is sane, including the validity of the transaction **
        // Some basic self checks are performed. It is possible to violate these when constructing a
        // block in code or getting a block from the database. The wire format is such that it is
        // impossible to hit many of these for blocks that went over the network.

        ValidationResult txValidation = validateTransaction(block, db);
        if(txValidation.getStatus() != ValidationResult.VALID) {
            result.setStatus(txValidation.getStatus());
            for (String error : txValidation.getErrors()) {
                errors.add(error);
            }
        }

        if(block.getSequenceNumber() < GENESIS_SEQ) {
            result.setInvalid();
            errors.add("Sequence number is prior to genesis. Number is now" + block.getSequenceNumber() + ", genesis: " + GENESIS_SEQ);
        }
        if(block.getLinkSequenceNumber() < GENESIS_SEQ && block.getLinkSequenceNumber() != UNKNOWN_SEQ) {
            result.setInvalid();
            errors.add("Link sequence number not empty and is prior to genesis");
        }

        //TODO: resolve stupid conversions byte[] => Base64 => byte[]
        String key = Base64.encodeToString(block.getPublicKey().toByteArray(), Base64.DEFAULT);
        PublicKey publicKey = Key.loadPublicKey(key);
        if(publicKey == null) {
            result.setInvalid();
            errors.add("Public key is not valid");
            Log.e("BLOCK", "Public key invalid\n" + key);
        } else {
            // If public key is valid, check validity of signature
            byte[] hash = hash(block);
            byte[] signature = block.getSignature().toByteArray();
            if (!Key.verify(publicKey, hash, signature)) {
                result.setInvalid();
                errors.add("Invalid signature.");
                Log.e("BLOCK", "Invalid signature!!!");
            } else {
                Log.e("BLCK", "Valid signature");
            }
        }

        // If a block is linked with a block of the same owner it does not serve any purpose and is invalid.
        if(block.getPublicKey().equals(block.getLinkPublicKey())) {
            result.setInvalid();
            errors.add("Self linked block");
        }
        // If it is implied that block is a genesis block, check if it correctly set up
        if(isGenesisBlock(block)){
            if(block.getSequenceNumber() == GENESIS_SEQ && !block.getPreviousHash().equals(GENESIS_HASH)) {
                result.setInvalid();
                errors.add("Sequence number implies previous hash should be Genesis Hash");
            }
            if(block.getSequenceNumber() != GENESIS_SEQ && block.getPreviousHash().equals(GENESIS_HASH)) {
                result.setInvalid();
                errors.add("Sequence number implies previous hash should not be Genesis Hash");
            }
        }

        // ** Step 4: does the database already know about this block? **
        // If so it should be equal or else we caught a branch in someones trustchain.
        if(dbBlock != null) {
            // Sanity check to see if database returned the expected block, we want to make sure we
            // have the right block before making a fraud claim.
            if(!dbBlock.getPublicKey().equals(block.getPublicKey()) ||
                    dbBlock.getSequenceNumber() != block.getSequenceNumber()) {
                throw new Exception("Database returned unexpected block");
            }
            if(!dbBlock.getLinkPublicKey().equals(block.getLinkPublicKey())) {
                result.setInvalid();
                errors.add("Link public key does not match known block.");
            }
            if(dbBlock.getLinkSequenceNumber() != block.getLinkSequenceNumber()) {
                result.setInvalid();
                errors.add("Link sequence number does not match known block.");
            }
            if(!dbBlock.getPreviousHash().equals(block.getPreviousHash())) {
                result.setInvalid();
                errors.add("Previous hash does not match known block.");

            }
            if(!dbBlock.getSignature().equals(block.getSignature())) {
                result.setInvalid();
                errors.add("Signature does not match known block");
            }
            // If the known block is not equal to block in db, and the signatures are valid, we have
            // a double signed PK/seqNum. Fraud!
            if(!hash(dbBlock).equals(hash(block)) && !errors.contains("Invalid signature") &&
                    !errors.contains("Public key not valid")) {
                result.setInvalid();
                errors.add("Double sign fraud");
            }
        }

        // ** Step 5: Does the database have the linked block? **
        // If so, do the values match up? If the values do not match up someone committed fraud, but
        // it is impossible to know who. So we just invalidate the block that is the latter to get
        // validated. We can also detect double counter sign fraud at this point.
        if(linkBlock != null) {
            // Sanity check to see if the database returned the expected block, we want to make sure
            // we have the right block before making a fraud claim.
            if(!linkBlock.getPublicKey().equals(block.getPublicKey()) ||
                    (linkBlock.getLinkSequenceNumber() != block.getSequenceNumber() &&
                    linkBlock.getSequenceNumber() != block.getLinkSequenceNumber())) {
                throw new Exception("Database returned unexpected block");
            }
            if(!block.getPublicKey().equals(linkBlock.getPublicKey())) {
                result.setInvalid();
                errors.add("Public key mismatch on linked block");
            } else if(block.getLinkSequenceNumber() != UNKNOWN_SEQ) {
                // Self counter signs another block (link). If linkBlock has a linked block that is not
                // equal to block, then block is fraudulent, since it tries to countersign a block
                // that is already countersigned.
                MessageProto.TrustChainBlock linkLinkBlock = getBlock(db,
                        linkBlock.getLinkPublicKey().toByteArray(), linkBlock.getLinkSequenceNumber());
                if(linkLinkBlock != null && !Arrays.equals(hash(linkLinkBlock), hash(block))) {
                    result.setInvalid();
                    errors.add("Double countersign fraud");
                }
            }
        }

        // ** Step 6: Did we get blocks from the database before or after block? **
        // They should be checked for violations too.
        if(prevBlock != null) {
            // Sanity check of the previous block the database gave us
            if(!prevBlock.getPublicKey().equals(block.getPublicKey()) ||
                    prevBlock.getSequenceNumber() >= block.getSequenceNumber()) {
                throw new Exception("Database returned unexpected block");
            }
            // If there is no gap, the previous hash should be equal to the hash of prevBlock
            if(prevBlock.getSequenceNumber() == block.getSequenceNumber() - 1 &&
                    !Arrays.equals(block.getPreviousHash().toByteArray(), hash(prevBlock))) {
                result.setInvalid();
                errors.add("Previous hash is not equal to the hash id of the previous block");
                // Is this fraud? It is certainly an error, but fixing it would require a different
                // signature on the same sequence number, which would be fraud.
            }
        }
        if(nextBlock != null) {
            // Sanity check of the previous block the database gave us
            if(!nextBlock.getPublicKey().equals(block.getPublicKey()) ||
                    nextBlock.getSequenceNumber() <= block.getSequenceNumber()) {
                throw new Exception("Database returned unexpected block");
            }
            // If there is no gap, the previous hash of nextBlock should be equal to the hash of block
            if(nextBlock.getSequenceNumber() == block.getSequenceNumber() + 1 &&
                    !Arrays.equals(nextBlock.getPreviousHash().toByteArray(), hash(block))) {
                result.setInvalid();
                errors.add("Next hash is not equal to the hash id of the block");
                // Again, this might not be fraud, but fixing it can only result in fraud.
            }
        }

        return result.setErrors(errors);
    }

    /**
     * Retrieves the block associated with the given public key and sequence number from the database
     * @param dbReadable - Database to search in
     * @param pubkey - Public key of which the latest block should be found
     * @param seqNumber - Int value of the sequence number of the block to be retrieved
     * @return The latest block in the database or null if something went wrong
     */
    public static MessageProto.TrustChainBlock getBlock(SQLiteDatabase dbReadable, byte[] pubkey, int seqNumber) {
        MessageProto.TrustChainBlock res = null;
        String whereClause = TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY + " = ? AND " +
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " = ?";
        String[] whereArgs = new String[] {ByteString.copyFrom(pubkey).toStringUtf8(),
                Integer.toString(seqNumber)};

        Cursor cursor = dbReadable.query(
                TrustChainDBContract.BlockEntry.TABLE_NAME,     // Table name for the query
                null,                                           // The columns to return, in this case all columns
                whereClause,                                    // Filter for which rows to return
                whereArgs,                                      // Filter arguments
                null,                                           // Declares how to group rows
                null,                                           // Declares which row groups to include
                null                                            // How the rows should be ordered
        );
        if(cursor.getCount() == 1) {
            cursor.moveToFirst();

            res = MessageProto.TrustChainBlock.newBuilder().setTransaction(ByteString.copyFromUtf8(cursor.getString(
                    cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_TX))))
                    .setPublicKey(ByteString.copyFromUtf8(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY))))
                    .setSequenceNumber(cursor.getInt(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER)))
                    .setLinkPublicKey(ByteString.copyFromUtf8(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_PUBLIC_KEY))))
                    .setLinkSequenceNumber(cursor.getInt(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_SEQUENCE_NUMBER)))
                    .setPreviousHash(ByteString.copyFromUtf8(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_PREVIOUS_HASH))))
                    .setSignature(ByteString.copyFromUtf8(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_SIGNATURE))))
                    .build();
        }
        cursor.close();
        return res;
    }

    /**
     * Returns the block with the highest sequence number smaller than the given sequence number and
     * the same public key: the previous block in the chain. Sequence number is allowed to be another
     * value than seqNumber - 1.
     * @param dbReadable - Database from which to read
     * @param pubkey - Public key of the block of which to find the previous block in the chain
     * @param seqNumber - Sequence number of block of which to find the previous block in the chain
     * @return The previous TrustChainBlock in the chain
     */
    public static MessageProto.TrustChainBlock getBlockBefore(SQLiteDatabase dbReadable, byte[] pubkey, int seqNumber){
        MessageProto.TrustChainBlock res = null;
        String whereClause = TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY + " = ? AND " +
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " < ?";
        String[] whereArgs = new String[] {ByteString.copyFrom(pubkey).toStringUtf8(),
                Integer.toString(seqNumber)};
        String orderBy = TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " DESC";

        Cursor cursor = dbReadable.query(
                TrustChainDBContract.BlockEntry.TABLE_NAME,     // Table name for the query
                null,                                           // The columns to return, in this case all columns
                whereClause,                                    // Filter for which rows to return
                whereArgs,                                      // Filter arguments
                null,                                           // Declares how to group rows
                null,                                           // Declares which row groups to include
                orderBy                                            // How the rows should be ordered
        );
        if(cursor.getCount() >= 1) {
            cursor.moveToFirst();

            res = MessageProto.TrustChainBlock.newBuilder().setTransaction(ByteString.copyFromUtf8(cursor.getString(
                    cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_TX))))
                    .setPublicKey(ByteString.copyFromUtf8(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY))))
                    .setSequenceNumber(cursor.getInt(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER)))
                    .setLinkPublicKey(ByteString.copyFromUtf8(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_PUBLIC_KEY))))
                    .setLinkSequenceNumber(cursor.getInt(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_SEQUENCE_NUMBER)))
                    .setPreviousHash(ByteString.copyFromUtf8(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_PREVIOUS_HASH))))
                    .setSignature(ByteString.copyFromUtf8(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_SIGNATURE))))
                    .build();
        }
        cursor.close();
        return res;
    }

    /**
     * Returns the block with the lowest sequence number greater than the given sequence number and
     * the same public key: The next block in the chain. Sequence number is allowed to be another
     * value than seqNumber + 1.
     * @param dbReadable - Database from which to read
     * @param pubkey - Public key of the block of which to find the previous block in the chain
     * @param seqNumber - Sequence number of block of which to find the previous block in the chain
     * @return The next TrustChainBlock in the chain
     */
    public static MessageProto.TrustChainBlock getBlockAfter(SQLiteDatabase dbReadable, byte[] pubkey, int seqNumber){
        MessageProto.TrustChainBlock res = null;
        String whereClause = TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY + " = ? AND " +
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " > ?";
        String[] whereArgs = new String[] {ByteString.copyFrom(pubkey).toStringUtf8(),
                Integer.toString(seqNumber)};
        String orderBy = TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " ASC";

        Cursor cursor = dbReadable.query(
                TrustChainDBContract.BlockEntry.TABLE_NAME,     // Table name for the query
                null,                                           // The columns to return, in this case all columns
                whereClause,                                    // Filter for which rows to return
                whereArgs,                                      // Filter arguments
                null,                                           // Declares how to group rows
                null,                                           // Declares which row groups to include
                orderBy                                         // How the rows should be ordered
        );
        if(cursor.getCount() >= 1) {
            cursor.moveToFirst();

            res = MessageProto.TrustChainBlock.newBuilder().setTransaction(ByteString.copyFromUtf8(cursor.getString(
                    cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_TX))))
                    .setPublicKey(ByteString.copyFromUtf8(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY))))
                    .setSequenceNumber(cursor.getInt(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER)))
                    .setLinkPublicKey(ByteString.copyFromUtf8(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_PUBLIC_KEY))))
                    .setLinkSequenceNumber(cursor.getInt(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_SEQUENCE_NUMBER)))
                    .setPreviousHash(ByteString.copyFromUtf8(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_PREVIOUS_HASH))))
                    .setSignature(ByteString.copyFromUtf8(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_SIGNATURE))))
                    .build();
        }
        cursor.close();
        return res;
    }

    /**
     * Returns the latest block in the database associated with the given public key.
     * @param dbReadable - database in which to search
     * @param pubkey - public key for which to search for blocks
     * @return
     */
    public static MessageProto.TrustChainBlock getLatestBlock(SQLiteDatabase dbReadable, byte[] pubkey) {
        return getBlock(dbReadable,pubkey,getMaxSeqNum(dbReadable,pubkey));
    }

    /**
     * Get the maximum sequence number in the database associated with the given public key
     * @param dbReadable - database in which to search
     * @param pubkey - public key for which to search for blocks
     * @return the maximum sequence number found
     */
    public static int getMaxSeqNum(SQLiteDatabase dbReadable, byte[] pubkey) {
        int res = -1;
        String[] projection = new String[] {"max(" +
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + ")"};
        String whereClause = TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY + " = ?";
        String[] whereArgs = new String[] {ByteString.copyFrom(pubkey).toStringUtf8()};

        Cursor cursor = dbReadable.query(
                TrustChainDBContract.BlockEntry.TABLE_NAME,
                projection,
                whereClause,
                whereArgs,
                null,
                null,
                null
        );
        if(cursor.getCount() == 1) {
            cursor.moveToFirst();
            res = cursor.getInt(cursor.getColumnIndex(
                    "max(" + TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + ")"));
        }
        cursor.close();
        return res;
    }

    /**
     * Validates the transaction of a block, for now a transaction can be anything so no validation
     * method is implemented.
     * @param block - The block containing the to-be-checked transaction.
     * @param db - Database to validate against
     * @return
     */
    public static ValidationResult validateTransaction(MessageProto.TrustChainBlock block, SQLiteDatabase db) {
        return new ValidationResult();
    }

}
