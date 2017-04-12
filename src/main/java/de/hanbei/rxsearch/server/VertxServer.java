package de.hanbei.rxsearch.server;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.ning.http.client.AsyncHttpClient;
import de.hanbei.rxsearch.config.SearcherConfiguration;
import de.hanbei.rxsearch.events.VertxEventVerticle;
import de.hanbei.rxsearch.filter.OfferProcessor;
import de.hanbei.rxsearch.metrics.Measured;
import de.hanbei.rxsearch.searcher.Searcher;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.ResponseTimeHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VertxServer extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxServer.class);

    private final AsyncHttpClient asyncHttpClient;
    private final List<Searcher> searchers;
    private final List<OfferProcessor> processors;

    private SearchRouter searchRouter;
    private HttpServer httpServer;


    public VertxServer() {
        asyncHttpClient = new AsyncHttpClient();
        SearcherConfiguration searcherConfiguration = new SearcherConfiguration(asyncHttpClient);

        searchers = searcherConfiguration.loadConfiguration("rxsearch", "testing", "de");
        processors = Lists.newArrayList();


        ConsoleReporter reporter = ConsoleReporter.forRegistry(SharedMetricRegistries.getOrCreate(Measured.SEARCHER_METRICS))
                .shutdownExecutorOnStop(true).build();
        //reporter.start(5, TimeUnit.SECONDS);
    }

    @Override
    public void start(Future<Void> fut) {
        searchRouter = new SearchRouter(searchers, processors, vertx.eventBus());

        httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);

        router.route().handler(LoggerHandler.create());
        router.route().handler(ResponseTimeHandler.create());


        router.route("/search/offers").handler(BodyHandler.create());
        router.route("/search/offers").handler(TimeoutHandler.create());
        router.post("/search/offers").handler(searchRouter);

        router.route().handler(StaticHandler.create()
                .setWebRoot("apidocs")
                .setFilesReadOnly(false)
                .setCachingEnabled(false));

        Integer port = port();
        httpServer.requestHandler(router::accept).listen(port, result -> {
            if (result.succeeded()) {
                fut.complete();
                LOGGER.info("Started server on {}", port);
            } else {
                LOGGER.info("Failed starting server: {}", result.cause());
                fut.fail(result.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        LOGGER.info("Stopping server");
        asyncHttpClient.close();
        httpServer.close();
        super.stop(stopFuture);
    }

    private Integer port() {
        Integer port = 8080;

        String portAsString = System.getenv("PORT");
        if (!Strings.isNullOrEmpty(portAsString)) {
            port = Integer.parseInt(portAsString);
        } else if (config().containsKey("http.port")) {
            port = config().getInteger("http.port");
        }
        return port;
    }

    public static void main(String[] args) throws IOException {
        Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new DropwizardMetricsOptions().setEnabled(true).setJmxEnabled(true)));

        Json.mapper.registerModule(new GuavaModule());
        Json.mapper.registerModule(new KotlinModule());
        Json.prettyMapper.registerModule(new GuavaModule());
        Json.prettyMapper.registerModule(new KotlinModule());

        Stopwatch stopwatch = Stopwatch.createStarted();
        vertx.deployVerticle(VertxEventVerticle.class.getName(), r1 -> {
            if (r1.succeeded()) {
                vertx.deployVerticle(VertxServer.class.getName(), r2 -> {
                    if (r2.succeeded()) {
                        stopwatch.stop();
                        LOGGER.info("Startup time {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
                    }
                });
            }
        });


        Runtime.getRuntime().addShutdownHook(new Thread(vertx::close));
    }

}
