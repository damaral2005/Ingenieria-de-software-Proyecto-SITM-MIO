package edu.icesi.sitmmio.distributed;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.InputStream;
import com.zeroc.Ice.OutputStream;

final class IceInvocationCodec {
    private IceInvocationCodec() {
    }

    static byte[] writeString(Communicator communicator, String value) {
        OutputStream outputStream = new OutputStream(communicator);
        outputStream.startEncapsulation();
        outputStream.writeString(value);
        outputStream.endEncapsulation();
        return outputStream.finished();
    }

    static String readString(Communicator communicator, byte[] bytes) {
        InputStream inputStream = new InputStream(communicator, bytes);
        inputStream.startEncapsulation();
        String value = inputStream.readString();
        inputStream.endEncapsulation();
        return value;
    }
}
