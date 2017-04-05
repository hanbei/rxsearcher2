package de.hanbei.rxsearch.coordination;

import de.hanbei.rxsearch.model.Offer;
import de.hanbei.rxsearch.searcher.SearcherException;
import io.reactivex.Observable;

@FunctionalInterface
public interface SearcherErrorHandler {

    Observable<Offer> searcherError(SearcherException t);

}
