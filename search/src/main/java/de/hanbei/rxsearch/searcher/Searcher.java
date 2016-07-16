package de.hanbei.rxsearch.searcher;

import de.hanbei.rxsearch.model.Offer;
import de.hanbei.rxsearch.model.Query;
import rx.Observable;

public interface Searcher {
    String getName();

    Observable<Offer> search(Query query);
}
