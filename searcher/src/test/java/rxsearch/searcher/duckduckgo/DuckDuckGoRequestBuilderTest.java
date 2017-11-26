package rxsearch.searcher.duckduckgo;

import rxsearch.model.Query;
import rxsearch.searcher.RequestUrlBuilderTest;
import okhttp3.Request;
import org.junit.Before;
import org.junit.Test;
import rxsearch.searcher.SearcherRequest;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DuckDuckGoRequestBuilderTest extends RequestUrlBuilderTest {

    @Before
    public void setUp() {
        urlBuilder = new DuckDuckGoRequestBuilder();
    }

    @Test
    public void correctRequestUrlIsBuilt() {
        SearcherRequest request = urlBuilder.createRequest(Query.builder().keywords("input").requestId("id").country("de").build());
        assertThat(request.url(), is("http://api.duckduckgo.com/?format=json&t=hanbeirxsearch&q=input"));
    }

}