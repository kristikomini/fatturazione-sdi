package it.kristikomini.fatturapa.sdi;

/** Raised when an invoice is asked to move along an edge the SDI lifecycle does not allow. */
public class InvalidSdiTransitionException extends RuntimeException {

    public InvalidSdiTransitionException(SdiState from, SdiState to) {
        super("Illegal SDI transition: " + from + " -> " + to);
    }
}
