import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Test;

import java.util.Collections;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class SuggestionsTest{

    String indexName = "suggestions-bug";
    String documentType = "song";
    String documentId = "1";

    @Test
    public void testSuggestions() throws Exception {

        Client client = new TransportClient()
                .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

        final IndicesExistsResponse res = client.admin().indices().prepareExists(indexName).execute().actionGet();
        if (res.isExists()) {
            final DeleteIndexRequestBuilder delIdx = client.admin().indices().prepareDelete(indexName);
            delIdx.execute().actionGet();
        }

        final CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);

        final XContentBuilder mappingBuilder = jsonBuilder().prettyPrint()
                .startObject()
                    .startObject("properties")
                        .startObject("name")
                            .field("type", "string")
                        .endObject()
                        .startObject("suggest")
                            .field("type", "completion")
                            .field("payloads", true)
                        .endObject()
                    .endObject()
                .endObject();

        System.out.println("Adding mapping");
        System.out.println(mappingBuilder.string());
        createIndexRequestBuilder.addMapping(documentType, mappingBuilder);

        createIndexRequestBuilder.execute().actionGet();

        // Add documents
        final IndexRequestBuilder indexRequestBuilder = client.prepareIndex(indexName, documentType, documentId);
        // build json object
        final XContentBuilder contentBuilder = jsonBuilder().prettyPrint()
                .startObject()
                    .field("name", "Queen")
                    .startObject("suggest")
                        .field("input", Collections.singletonList("Queen"))
                        .field("output", "Queen")
                        .startObject("payload")
                            .field("country", "GB")
                        .endObject()
                    .endObject()
                .endObject();

        System.out.println("Indexing document");
        System.out.println(contentBuilder.string());

        indexRequestBuilder.setSource(contentBuilder);
        indexRequestBuilder.execute().actionGet();

        // Get document


        assertThat("foo", is("bar"));
    }
}
