import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class SuggestionsTest{

    String indexName = "suggestions-bug";
    String documentType = "band";
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

        // Index document
        final IndexRequestBuilder indexRequestBuilder = client.prepareIndex(indexName, documentType, documentId);
        // build json object
        final XContentBuilder indexContentBuilder = jsonBuilder().prettyPrint()
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
        System.out.println(indexContentBuilder.string());

        indexRequestBuilder.setSource(indexContentBuilder);
        indexRequestBuilder.execute().actionGet();

        // Update payload
        final UpdateRequestBuilder updateRequestBuilder = client.prepareUpdate(indexName, documentType, documentId);
        // build json object
        final XContentBuilder updateContentBuilder = jsonBuilder().prettyPrint()
                .startObject()
                    .startObject("doc")
                        .startObject("suggest")
                            .startObject("payload")
                                .field("country", "DE")
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();

        System.out.println("Updating document");
        System.out.println(updateContentBuilder.string());

        updateRequestBuilder.setSource(updateContentBuilder);
        updateRequestBuilder.execute().actionGet();

        GetResponse getResponse = client.prepareGet(indexName, documentType, documentId)
                .execute()
                .actionGet();

        System.out.println("Waiting one second for ES to finish indexing");
        Thread.sleep(1000);
        System.out.println("Fetching document. Response is");
        System.out.println(getResponse.getSourceAsString());

        Map suggest = (Map<String, Map<String, String>>)getResponse.getSourceAsMap().get("suggest");
        Map<String, String> payload = (Map<String, String>)suggest.get("payload");
        String getCountry = payload.get("country");

        System.out.println("The payload country in the document is " + getCountry);

        CompletionSuggestionBuilder suggestionsBuilder = new CompletionSuggestionBuilder("suggest");
        suggestionsBuilder.text("Queen");
        suggestionsBuilder.field("suggest");
        SuggestRequestBuilder suggestRequestBuilder = client.prepareSuggest(indexName).addSuggestion(suggestionsBuilder);
        SuggestResponse suggestResponse = suggestRequestBuilder.execute().actionGet();

        System.out.println("Suggestion response is");
        System.out.println(suggestResponse.getSuggest());

        CompletionSuggestion.Entry.Option option = (CompletionSuggestion.Entry.Option)
            suggestResponse.getSuggest().getSuggestion("suggest").getEntries().get(0).getOptions().get(0);
        String suggestCountry = (String) option.getPayloadAsMap().get("country");

        assertThat(getCountry, is(suggestCountry));
    }
}
