import MessageVerifierTest.TestUtility;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;

public class MessageVerifierC10KTest {

    private static final String TestServerHost = "localhost";
    private static final int TestServerPort = 8080;
    private static final int RequestCount = 10000;
    private static final InetSocketAddress InetSocketAddress = new InetSocketAddress(TestServerHost, TestServerPort);

    public static void main(String args[]) {
        int i;
        Thread threads[];
        CountDownLatch requestsLatch, successLatch;

        threads = new Thread[RequestCount];
        requestsLatch = new CountDownLatch(RequestCount);
        successLatch = new CountDownLatch(RequestCount);

        for (i = 0; i < RequestCount; i++) {
            threads[i] = new Thread(new SimpleTest(requestsLatch, successLatch));
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
        private CountDownLatch requestsLatch, successLatch;

        private SimpleTest(CountDownLatch requestsLatch, CountDownLatch successLatch) {
            this.requestsLatch = requestsLatch;
            this.successLatch = successLatch;
        }

        @Override
        public void run() {
            try {
                if (TestUtility.singleRequestTest(InetSocketAddress)) {
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
