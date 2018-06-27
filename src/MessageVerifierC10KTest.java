import MessageVerifierTest.TestUtility;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;

public class MessageVerifierC10KTest {

    private static final String TestServerHost = "localhost";
    private static final int TestServerPort = 8080;
    private static final int RequestCount = 10000;

    public static void main(String args[]) {
        String givenHost;
        int givenPort, givenThreadCount, i;
        InetSocketAddress inetSocketAddress;

        givenHost = TestServerHost;
        givenPort = TestServerPort;
        givenThreadCount = RequestCount;

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

        for (i = 0; i < givenThreadCount; i++) {
            threads[i] = new Thread(new SimpleTest(inetSocketAddress, requestsLatch, successLatch));
            threads[i].start();
        }

        try {
            requestsLatch.await();
            assert 0 == successLatch.getCount();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            assert false;
        }
    }

    private static class SimpleTest implements Runnable {
        private InetSocketAddress inetSocketAddress;
        private CountDownLatch requestsLatch, successLatch;

        private SimpleTest(InetSocketAddress inetSocketAddress, CountDownLatch requestsLatch, CountDownLatch successLatch) {
            this.inetSocketAddress = inetSocketAddress;
            this.requestsLatch = requestsLatch;
            this.successLatch = successLatch;
        }

        @Override
        public void run() {
            try {
                if (TestUtility.singleRequestTest(this.inetSocketAddress)) {
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
