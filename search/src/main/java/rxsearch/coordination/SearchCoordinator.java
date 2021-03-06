package rxsearch.coordination;

import rxsearch.model.Hit;
import rxsearch.model.Query;
import rxsearch.searcher.Searcher;
import rxsearch.searcher.SearcherException;
import io.reactivex.Observable;

import java.util.List;

public class SearchCoordinator {

    private final List<Searcher> searchers;
    private final SearcherErrorHandler onError;
    private final SearcherCompletionHandler onCompleted;
    private final SearcherResult onNext;

    public SearchCoordinator(List<Searcher> searcher) {
        this(searcher, SearchCoordinator::noop, SearchCoordinator::noop, SearchCoordinator::noop);
    }

    public SearchCoordinator(List<Searcher> searcher, SearcherErrorHandler onError) {
        this(searcher, onError, SearchCoordinator::noop, SearchCoordinator::noop);
    }

    public SearchCoordinator(List<Searcher> searcher, SearcherErrorHandler onError, SearcherCompletionHandler onCompleted) {
        this(searcher, onError, onCompleted, SearchCoordinator::noop);
    }

    public SearchCoordinator(List<Searcher> searcher, SearcherErrorHandler onError, SearcherCompletionHandler onCompleted, SearcherResult resultHandler) {
        this.searchers = searcher;
        this.onError = onError;
        this.onCompleted = onCompleted;
        this.onNext = resultHandler;
    }

    public Observable<Hit> startSearch(Query query) {
        return Observable.fromIterable(searchers)
                .flatMap(
                        searcher -> searcher.search(query)
                                .doOnNext(offer -> onNext.searcherResult(query.getRequestId(), searcher.getName(), offer))
                                .doOnComplete(() -> onCompleted.searcherCompleted(query.getRequestId(), searcher.getName(), query))
                                .doOnError(t -> onError.searcherError(query.getRequestId(), searcher.getName(), SearcherException.wrap(t).searcher(searcher.getName()).query(query)))
                                .onErrorResumeNext(Observable.empty())
                ).map(offer -> Hit.from(offer).requestId(query.getRequestId()).build());
    }

    private static <T> void noop(String r, String s, T t) {
    }
}
