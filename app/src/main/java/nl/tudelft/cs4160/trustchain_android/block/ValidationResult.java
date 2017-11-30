package nl.tudelft.cs4160.trustchain_android.block;

import java.util.List;

import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.ValidationStatus.INVALID;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.ValidationStatus.NO_INFO;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.ValidationStatus.PARTIAL;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.ValidationStatus.PARTIAL_NEXT;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.ValidationStatus.PARTIAL_PREVIOUS;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.ValidationStatus.VALID;

public class ValidationResult {
    ValidationStatus status;
    List<String> errors;

    public enum ValidationStatus {
        VALID, PARTIAL, PARTIAL_NEXT, PARTIAL_PREVIOUS, NO_INFO, INVALID
    }

    /**
     * Constructor, we always start out thinking everything is fine.
     */
    public ValidationResult() {
        status = VALID;
    }

    /**
     * The block does not violate any rules, but there are gaps between this and previous or next
     * blocks, or it does not have previous or next blocks.
     *
     * @return the updated validation result
     */
    public ValidationResult setPartial() {
        this.status = PARTIAL;
        return this;
    }

    /**
     * The block does not violate any rules, but there are gaps between this and the next block
     * or no next blocks.
     *
     * @return the updated validation result
     */
    public ValidationResult setPartialNext() {
        this.status = PARTIAL_NEXT;
        return this;
    }

    /**
     * The block does not violate any rules, but there are gaps between this and the previous block
     * or no previous blocks.
     *
     * @return the updated validation result
     */
    public ValidationResult setPartialPrevious() {
        this.status = PARTIAL_PREVIOUS;
        return this;
    }

    /**
     * There are no blocks (previous or next) to validate against.
     *
     * @return the updated validation result
     */
    public ValidationResult setNoInfo() {
        this.status = NO_INFO;
        return this;
    }

    /**
     * The block violates at least one validation rule.
     *
     * @return the updated validation result
     */
    public ValidationResult setInvalid() {
        this.status = INVALID;
        return this;
    }

    /**
     * General method for setting the status.
     *
     * @param newStatus - Integer representing the new status
     * @return the updated validation result
     */
    public ValidationResult setStatus(ValidationStatus newStatus) {
        this.status = newStatus;
        return this;
    }

    public ValidationStatus getStatus() {
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
     *
     * @return String representation of this ValidationResult
     */
    public String toString() {
        return "<ValidationResult: " + status + ">";
    }

}
