package nl.tudelft.cs4160.trustchain_android.block;

import java.util.List;

/**
 * Created by wkmeijer on 18-10-17.
 */

public class ValidationResult {
    int status;
    List<String> errors;

    public final static int VALID = 0;
    public final static int PARTIAL = 1;
    public final static int PARTIAL_NEXT = 2;
    public final static int PARTIAL_PREVIOUS = 3;
    public final static int NO_INFO = 4;
    public final static int INVALID = 5;

    /**
     * Constructor, we always start out thinking everything is fine.
     */
    public ValidationResult() {
        status = VALID;
    }

    /**
     * The block does not violate any rules, but there are gaps between this and previous or next
     * blocks, or it does not have previous or next blocks.
     * @return
     */
    public ValidationResult setPartial(){
        this.status = PARTIAL;
        return this;
    }

    /**
     * The block does not violate any rules, but there are gaps between this and the next block
     * or no next blocks.
     * @return
     */
    public ValidationResult setPartialNext(){
        this.status = PARTIAL_NEXT;
        return this;
    }

    /**
     * The block does not violate any rules, but there are gaps between this and the previous block
     * or no previous blocks.
     * @return
     */
    public ValidationResult setPartialPrevious(){
        this.status = PARTIAL_PREVIOUS;
        return this;
    }

    /**
     * There are no blocks (previous or next) to validate against.
     * @return
     */
    public ValidationResult setNoInfo(){
        this.status = NO_INFO;
        return this;
    }

    /**
     * The block violates at least one validation rule.
     * @return
     */
    public ValidationResult setInvalid(){
        this.status = INVALID;
        return this;
    }

    /**
     * General method for setting the status.
     * @param newStatus - Integer representing the new status
     * @return
     */
    public ValidationResult setStatus(int newStatus){
        if(0 <= newStatus && status < 6) {
            this.status = newStatus;
            return this;
        } else {
            return null;
        }
    }

    public int getStatus(){
        return status;
    }

    public List<String> getErrors() {
        return errors;
    }

    public ValidationResult setErrors(List<String> errors) {
        this.errors = errors;
        return this;
    }

    /**
     * Creates a string representation of the ValidationResult, based on its status.
     * @return String representation of this ValidationResult
     */
    public String toString() {
        String res = "<ValidationResult: ";
        switch(status){
            case VALID: res += "VALID";
                break;
            case PARTIAL: res += "PARTIAL";
                break;
            case PARTIAL_NEXT: res += "PARTIAL_NEXT";
                break;
            case PARTIAL_PREVIOUS: res += "PARTIAL_PREVIOUS";
                break;
            case NO_INFO: res += "NO_INFO";
                break;
            case INVALID: res += "INVALID";
                break;
        }

        return res + ">";
    }

}
