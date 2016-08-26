package de.hanbei.rxsearch.coordination;

import com.google.common.collect.Lists;
import de.hanbei.rxsearch.model.Offer;
import de.hanbei.rxsearch.model.Query;
import de.hanbei.rxsearch.searcher.Searcher;
import de.hanbei.rxsearch.searcher.SearcherException;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchCoordinatorTest {

    private static final String MESSAGE = "message";
    private static final Query QUERY = Query.builder().keywords("query").requestId("id").country("de").build();
    private static final Query OTHER_QUERY = Query.builder().keywords("q").requestId("i").country("de").build();
    private Searcher searcher;
    private Given given;

    @Before
    public void setUp() {
        given = new Given();
        searcher = mock(Searcher.class);
    }

    @Test
    public void searchReturnSucessfulCallsSuccessHandler() {
        SearchCoordinator coordinator = given.givenCoordinatorNotExpectingError(searcher);
        Offer offer = Offer.builder().url("").title("").price(0.0, "EUR").searcher("test").build();
        when(searcher.search(any(Query.class))).thenReturn(Observable.just(offer));

        TestSubscriber<Offer> subscriber = new TestSubscriber<>();
        coordinator.startSearch(QUERY).subscribe(subscriber);
        subscriber.assertCompleted();
        subscriber.assertValue(offer);
    }

    @Test
    public void searchThrowsCallsErrorHandler() {
        SearchCoordinator coordinator = given.givenCoordinatorNotExpectingError(searcher);
        when(searcher.search(any(Query.class))).thenThrow(new SearcherException(MESSAGE).query(OTHER_QUERY));

        TestSubscriber<Offer> subscriber = new TestSubscriber<>();
        coordinator.startSearch(QUERY).subscribe(subscriber);
        subscriber.assertNotCompleted();
        subscriber.assertNoValues();
    }

    @Test
    public void searchThrowsRuntimeExceptionIsWrappedInSearcherException() {
        SearchCoordinator coordinator = given.givenCoordinatorExpectingError(searcher, QUERY);
        when(searcher.search(any(Query.class))).thenReturn(Observable.error(new IllegalArgumentException(MESSAGE)));

        TestSubscriber<Offer> subscriber = new TestSubscriber<>();
        coordinator.startSearch(QUERY).subscribe(subscriber);
        subscriber.assertCompleted();
        subscriber.assertNoValues();
    }

    @Test
    public void searchThrowsSearcherExceptionIsNotWrappedInSearcherException() {
        SearchCoordinator coordinator = given.givenCoordinatorExpectingError(searcher, OTHER_QUERY);
        when(searcher.search(any(Query.class))).thenReturn(Observable.error(new SearcherException(MESSAGE).query(OTHER_QUERY)));

        coordinator.startSearch(QUERY);
    }

    private static class Given {
        SearchCoordinator givenCoordinatorExpectingError(Searcher searcher, Query query) {
            return new SearchCoordinator(Lists.newArrayList(searcher), t -> {
                assertThat(t, instanceOf(SearcherException.class));
                assertThat(t.getMessage(), containsString(MESSAGE));
                assertThat(t.getQuery(), is(query));
                assertThat(t.getCause(), instanceOf(IllegalArgumentException.class));
                return Observable.empty();
            });
        }

        SearchCoordinator givenCoordinatorNotExpectingError(Searcher searcher) {
            return new SearchCoordinator(Lists.newArrayList(searcher), t -> {
                fail("Should not throw error");
                return Observable.empty();
            });
        }


    }
}