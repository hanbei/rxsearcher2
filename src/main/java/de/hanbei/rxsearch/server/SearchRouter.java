package de.hanbei.rxsearch.server;

import de.hanbei.rxsearch.coordination.SearchCoordinator;
import de.hanbei.rxsearch.events.SearchEventHandler;
import de.hanbei.rxsearch.filter.OfferProcessor;
import de.hanbei.rxsearch.filter.OfferProcessorCoordinator;
import de.hanbei.rxsearch.model.Offer;
import de.hanbei.rxsearch.model.Query;
import de.hanbei.rxsearch.model.User;
import de.hanbei.rxsearch.searcher.Searcher;
import io.reactivex.Observable;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.Optional;

class SearchRouter implements Handler<RoutingContext> {

    private final SearchCoordinator searchCoordinator;
    private final OfferProcessorCoordinator filterCoordinator;

    public SearchRouter(List<Searcher> searcher, List<OfferProcessor> processors, EventBus eventBus) {
        SearchEventHandler eventHandler = new SearchEventHandler(eventBus);
        this.searchCoordinator = new SearchCoordinator(searcher, eventHandler, eventHandler, eventHandler);
        this.filterCoordinator = new OfferProcessorCoordinator(processors);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerRequest request = routingContext.request();
        String requestId = Optional.ofNullable(request.getHeader("X-Request-ID")).orElse("some_id");

        ResponseHandler responseHandler = new VertxResponseHandler(routingContext);

        Query q = extractQuery(routingContext, requestId);

        Observable<Offer> offerObservable = searchCoordinator.startSearch(q);

        filterCoordinator.filter(q, offerObservable).toList().subscribe(
                responseHandler::handleSuccess,
                responseHandler::handleError
        );
    }

    private Query extractQuery(RoutingContext routingContext, String requestId) {
        JsonObject queryAsJson = routingContext.getBodyAsJson();

        JsonObject query = queryAsJson.getJsonObject("query");

        User user = extractUser(query);

        Query.Companion.OtherStep builder = Query.builder().keywords(query.getString("keywords"))
                .requestId(requestId).country(query.getString("country")).user(user);

        return builder.build();
    }

    private User extractUser(JsonObject query) {
        JsonObject userJson = query.getJsonObject("user");
        return new User(userJson.getString("id"), userJson.getString("partnerId"), userJson.getString("partnerSubId"));
    }

}

