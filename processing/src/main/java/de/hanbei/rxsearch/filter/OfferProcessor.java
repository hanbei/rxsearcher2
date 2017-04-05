package de.hanbei.rxsearch.filter;

import de.hanbei.rxsearch.model.Offer;
import de.hanbei.rxsearch.model.Query;
import io.reactivex.Observable;

public interface OfferProcessor {

    Observable<Offer> process(Query query, Observable<Offer> observable);

}
