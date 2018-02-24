package main.tracker.response;

import main.tracker.Tracker;

import java.nio.ByteBuffer;

public class ErrorResponse extends TrackerResponse {
    final String errorMessage;

    //    int32_t	action	The action, in this case 3, for error. See actions.
    //    int32_t	transaction_id	Must match the transaction_id sent from the client.
    //    int8_t[]	error_string	The rest of the packet is a string describing the error.
    public ErrorResponse(Tracker tracker, byte[] response) {
        super(tracker);
        ByteBuffer receiveData = ByteBuffer.wrap(response);
        setActionNumber(receiveData.getInt());
        assert getActionNumber() == 3;
        setTransactionId(receiveData.getInt());
        //TODO: need to check in torrent specification what is the size of the error message in bytes.
        byte[] errorMessage = new byte[100];
        receiveData.get(errorMessage);
        this.errorMessage = new String(errorMessage);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static boolean isErrorResponse(byte[] response) {
        assert response.length > 0;
        return response[0] == 3;
    }
}
