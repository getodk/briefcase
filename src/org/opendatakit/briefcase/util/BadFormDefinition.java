package org.opendatakit.briefcase.util;

import org.opendatakit.aggregate.exception.ODKIncompleteSubmissionData.Reason;

public class BadFormDefinition extends Exception {

    private Reason reason;

    /**
     * Serial number for serialization
     */
    private static final long serialVersionUID = -8894929454515911356L;

    /**
     * Default constructor
     */
    public BadFormDefinition() {
        super();
        reason = Reason.UNKNOWN;
    }

    /**
     * Construct exception with the error message
     * 
     * @param message
     *            exception message
     */
    public BadFormDefinition(String message) {
        super(message);
        reason = Reason.UNKNOWN;
    }

    /**
     * Construction exception with error message and throwable cause
     * 
     * @param message
     *            exception message
     * @param cause
     *            throwable cause
     */
    public BadFormDefinition(String message, Throwable cause) {
        super(message, cause);
        reason = Reason.UNKNOWN;
    }

    /**
     * Construction exception with throwable cause
     * 
     * @param cause
     *            throwable cause
     */
    public BadFormDefinition(Throwable cause) {
        super(cause);
        reason = Reason.UNKNOWN;
    }

    /**
     * Default constructor with reason
     * 
     * @param exceptionReason
     *            exception reason
     */
    public BadFormDefinition(Reason exceptionReason) {
        super();
        reason = exceptionReason;
    }

    /**
     * Construct exception with the error message and reason
     * 
     * @param message
     *            exception message
     * @param exceptionReason
     *            exception reason
     */
    public BadFormDefinition(String message, Reason exceptionReason) {
        super(message);
        reason = exceptionReason;
    }

    /**
     * Construction exception with error message, throwable cause, and
     * reason
     * 
     * @param message
     *            exception message
     * @param cause
     *            throwable cause
     * @param exceptionReason
     *            exception reason
     */
    public BadFormDefinition(String message, Throwable cause,
            Reason exceptionReason) {
        super(message, cause);
        reason = exceptionReason;
    }

    /**
     * Construction exception with throwable cause and reason
     * 
     * @param cause
     *            throwable cause
     * @param exceptionReason
     *            exception reason
     */
    public BadFormDefinition(Throwable cause, Reason exceptionReason) {
        super(cause);
        reason = exceptionReason;
    }

    /**
     * Get the reason why the exception was generated
     * 
     * @return the reason
     */
    public Reason getReason() {
        return reason;
    }

 }