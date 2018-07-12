package MessageVerifierTest;

import Utility.RequestMessage;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;

public class TestUtility {
    private static final String TestChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789~!@#$%^&*()_+";
    private static final int TestCharsLength = TestChars.length();

    public static String generatedSHA512(String message) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        MessageDigest messageDigest;

        messageDigest = MessageDigest.getInstance("SHA-512");
        messageDigest.update(message.getBytes("utf8"));
        return String.format("%040x", new BigInteger(1, messageDigest.digest()));
    }

    private static String getRandomString(long count) {
        StringBuilder builder;

        builder = new StringBuilder();

        while (count-- != 0) {
            int index;

            index = ThreadLocalRandom.current().nextInt(0, TestCharsLength);
            builder.append(TestChars.charAt(index));
        }

        return builder.toString();
    }

    private static String getRandomMessageDataAsJSON(long length) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String randomString, checkSum;
        MVRequest MVRequest;

        randomString = getRandomString(length);

        checkSum = generatedSHA512(randomString);
        MVRequest = new MVRequest(randomString, checkSum);

        return new Gson().toJson(MVRequest);
    }

    private static String getRandomMessageAsJSON(long length) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String body;
        RequestMessage requestMessage;

        body = getRandomMessageDataAsJSON(length);

        requestMessage = new RequestMessage(body, "MessageVerifier");
        return new Gson().toJson(requestMessage);
    }

    private static ByteBuffer getRandomMessageByteBuffer(long length)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return ByteBuffer.wrap(getRandomMessageAsJSON(length).getBytes());
    }

    private static boolean messageSuccess(ByteBuffer byteBuffer) {
        String response;
        MVResponse messageMVResponse;
        byte validBytes[];
        int i;

        validBytes = new byte[byteBuffer.position()];

        for (i = 0; i< validBytes.length; i++) {
            validBytes[i] = byteBuffer.get(i);
        }

        response = new String(validBytes).trim();
        messageMVResponse = new Gson().fromJson(response, MVResponse.class);

        return messageMVResponse.getStatus();
    }

    public static boolean singleRequestTest(InetSocketAddress inetSocketAddress, long length)
            throws IOException, NoSuchAlgorithmException {
        ByteBuffer byteBuffer;
        SocketChannel socketChannel = null;
        boolean status;

        try {
            byteBuffer = TestUtility.getRandomMessageByteBuffer(length);
            socketChannel = SocketChannel.open(inetSocketAddress);
            while (byteBuffer.hasRemaining()) {
                socketChannel.write(byteBuffer);
            }
            byteBuffer.flip();
            socketChannel.read(byteBuffer);

            status = messageSuccess(byteBuffer);
        }
        finally {
            if (socketChannel != null) {
                socketChannel.close();
            }
        }

        return status;
    }
}
