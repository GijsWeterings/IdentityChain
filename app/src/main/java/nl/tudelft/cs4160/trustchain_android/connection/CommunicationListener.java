package nl.tudelft.cs4160.trustchain_android.connection;


/**
 * A listener, which can be used to report what the status is of send/received messages.
 */
public interface CommunicationListener {

    void updateLog(String msg);
}
