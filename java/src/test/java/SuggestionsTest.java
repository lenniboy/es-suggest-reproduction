import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class SuggestionsTest{

    @Test
    public void testSuggestions(){

        Client client = new TransportClient()
                .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));


        assertThat("foo", is("bar"));
    }
}
