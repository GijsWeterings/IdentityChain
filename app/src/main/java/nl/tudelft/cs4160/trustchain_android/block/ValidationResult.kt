package nl.tudelft.cs4160.trustchain_android.block

import nl.tudelft.cs4160.trustchain_android.block.ValidationResult.ValidationStatus.INVALID
import nl.tudelft.cs4160.trustchain_android.block.ValidationResult.ValidationStatus.NO_INFO
import nl.tudelft.cs4160.trustchain_android.block.ValidationResult.ValidationStatus.PARTIAL
import nl.tudelft.cs4160.trustchain_android.block.ValidationResult.ValidationStatus.PARTIAL_NEXT
import nl.tudelft.cs4160.trustchain_android.block.ValidationResult.ValidationStatus.PARTIAL_PREVIOUS
import nl.tudelft.cs4160.trustchain_android.block.ValidationResult.ValidationStatus.VALID

class ValidationResult {
    internal var status: ValidationStatus = VALID
    internal var errors: List<String> = emptyList()

    enum class ValidationStatus {
        VALID, PARTIAL, PARTIAL_NEXT, PARTIAL_PREVIOUS, NO_INFO, INVALID
    }

    /**
     * The block does not violate any rules, but there are gaps between this and previous or next
     * blocks, or it does not have previous or next blocks.
     *
     * @return the updated validation result
     */
    fun setPartial(): ValidationResult {
        this.status = PARTIAL
        return this
    }

    /**
     * The block does not violate any rules, but there are gaps between this and the next block
     * or no next blocks.
     *
     * @return the updated validation result
     */
    fun setPartialNext(): ValidationResult {
        this.status = PARTIAL_NEXT
        return this
    }

    /**
     * The block does not violate any rules, but there are gaps between this and the previous block
     * or no previous blocks.
     *
     * @return the updated validation result
     */
    fun setPartialPrevious(): ValidationResult {
        this.status = PARTIAL_PREVIOUS
        return this
    }

    /**
     * There are no blocks (previous or next) to validate against.
     *
     * @return the updated validation result
     */
    fun setNoInfo(): ValidationResult {
        this.status = NO_INFO
        return this
    }

    /**
     * The block violates at least one validation rule.
     *
     * @return the updated validation result
     */
    fun setInvalid(): ValidationResult {
        this.status = INVALID
        return this
    }

    /**
     * General method for setting the status.
     *
     * @param newStatus - Integer representing the new status
     * @return the updated validation result
     */
    fun setStatus(newStatus: ValidationStatus): ValidationResult {
        this.status = newStatus
        return this
    }

    fun getStatus(): ValidationStatus = status

    fun getErrors(): List<String> = errors

    fun setErrors(errors: List<String>): ValidationResult {
        this.errors = errors
        return this
    }

    /**
     * Creates a string representation of the ValidationResult, based on its status.
     *
     * @return String representation of this ValidationResult
     */
    override fun toString(): String = "<ValidationResult: $status>"


}
