package de.hanbei.rxsearch.coordination;

import de.hanbei.rxsearch.model.Query;

@FunctionalInterface
public interface SearcherCompletionHandler {

    void searcherCompleted(String searcher, Query query);

}