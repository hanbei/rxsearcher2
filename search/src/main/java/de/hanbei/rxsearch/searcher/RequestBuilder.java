package de.hanbei.rxsearch.searcher;

import com.ning.http.client.Request;
import de.hanbei.rxsearch.model.Query;

public interface RequestBuilder {

    /**
     * Takes the search parameter and builds the searcher request url. Any other parameters must be set during
     * construction.
     *
     * @param query The search terms. Must not be null or empty.
     * @return The searcher url that can be requested.
     */
    Request createRequest(Query query);
}
