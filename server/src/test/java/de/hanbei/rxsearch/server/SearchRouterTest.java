package de.hanbei.rxsearch.server;

import com.google.common.collect.Lists;
import de.hanbei.rxsearch.model.Offer;
import de.hanbei.rxsearch.searcher.Searcher;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchRouterTest {

    private SearchRouter router;
    private RoutingContext routingContext;

    private Searcher searcher1;
    private Searcher searcher2;

    private HttpServerRequest request;
    private HttpServerResponse response;

    @Before
    public void setUp() throws Exception {
        searcher1 = mock(Searcher.class);
        searcher2 = mock(Searcher.class);

        request = mock(HttpServerRequest.class);
        when(request.getParam("keyword")).thenReturn("search_term");
        response = mock(HttpServerResponse.class);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);

        routingContext = mock(RoutingContext.class);
        when(routingContext.request()).thenReturn(request);
        when(routingContext.response()).thenReturn(response);

        router = new SearchRouter(Lists.newArrayList(searcher1, searcher2));
    }

    @Test
    public void whenSearcherRespondRendersCorrectJson() throws Exception {
        when(searcher1.search("search_term")).thenReturn(Observable.just(Offer.builder().url("url").title("title").price(0.0, "USD").searcher("searcher1").build()));
        when(searcher2.search("search_term")).thenReturn(Observable.just(Offer.builder().url("url").title("title").price(0.0, "USD").searcher("searcher2").build()));

        router.handle(routingContext);

        verify(response, times(1)).end("{\"results\":["
                + "{\"url\":\"url\",\"title\":\"title\",\"price\":{\"amount\":0.0,\"currency\":\"USD\"},\"searcher\":\"searcher1\"},"
                + "{\"url\":\"url\",\"title\":\"title\",\"price\":{\"amount\":0.0,\"currency\":\"USD\"},\"searcher\":\"searcher2\"}"
                + "]}");
    }

    @Test
    public void sendsErrorResponseWhenSearchThrows() {
        when(searcher1.search("search_term")).thenReturn(Observable.error(new RuntimeException("SearchError")));

        router.handle(routingContext);

        verify(routingContext, times(1)).fail(any(RuntimeException.class));
    }

}