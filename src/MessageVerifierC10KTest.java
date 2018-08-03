import MessageVerifierTest.TestUtility;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MessageVerifierC10KTest {

    private static final String TestServerHost = "localhost";
    private static final int TestServerPort = 8080;
    private static final int RequestCount = 10000;
    private static final int MaxMessageLength = 1000;

    public static void main(String args[]) {
        String givenHost;
        int givenPort, givenThreadCount, i;
        long givenMaxMessageLength, startTime, endTime;
        InetSocketAddress inetSocketAddress;

        givenHost = TestServerHost;
        givenPort = TestServerPort;
        givenThreadCount = RequestCount;
        givenMaxMessageLength = MaxMessageLength;

        if (args.length > 0) {
            String previousKey = args[0];
            for(i = 1 ;i < args.length; i++){
                switch (previousKey) {
                    case "-host":
                        givenHost = args[i];
                        break;
                    case "-port":
                        givenPort = Integer.parseInt(args[i]);
                        break;
                    case "-threads":
                        givenThreadCount = Integer.parseInt(args[i]);
                        break;
                    case "-length":
                        givenMaxMessageLength = Long.parseLong(args[i]);
                        break;
                }
                previousKey = args[i];
            }
        }

        Thread threads[];
        CountDownLatch requestsLatch, successLatch;
        inetSocketAddress = new InetSocketAddress(givenHost, givenPort);

        threads = new Thread[givenThreadCount];
        requestsLatch = new CountDownLatch(givenThreadCount);
        successLatch = new CountDownLatch(givenThreadCount);

        startTime = System.nanoTime();

        for (i = 0; i < givenThreadCount; i++) {
            threads[i] = new Thread(new SimpleTest(inetSocketAddress, requestsLatch, successLatch, givenMaxMessageLength));
            threads[i].start();
        }

        try {
            requestsLatch.await();
            endTime = System.nanoTime();
            assert 0 == successLatch.getCount();

            long timeTaken;

            timeTaken = TimeUnit.SECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);

            System.out.print(String.format("Time taken for %d threads is %d seconds, length per msg was %d",
                    givenThreadCount, timeTaken, givenMaxMessageLength));
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            assert false;
        }
    }

    private static class SimpleTest implements Runnable {
        private InetSocketAddress inetSocketAddress;
        private CountDownLatch requestsLatch, successLatch;
        private long messageLength;

        private SimpleTest(InetSocketAddress inetSocketAddress, CountDownLatch requestsLatch,
                           CountDownLatch successLatch, long messageLength) {
            this.inetSocketAddress = inetSocketAddress;
            this.requestsLatch = requestsLatch;
            this.successLatch = successLatch;
            this.messageLength = messageLength;
        }

        @Override
        public void run() {
            try {
                if (TestUtility.singleRequestTest(this.inetSocketAddress, messageLength)) {
                    this.successLatch.countDown();
                }
            }
            catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            finally {
                this.requestsLatch.countDown();
            }
        }
    }

}
