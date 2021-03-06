package rxsearch.server;

import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import rxsearch.config.ConfigurationBuilder;
import rxsearch.events.LogSearchVerticle;
import rxsearch.events.LoggingVerticle;
import rxsearch.events.OfferProcessedEvent;
import rxsearch.events.SearchFailedEvent;
import rxsearch.events.SearchFinishedEvent;
import rxsearch.events.SearchStartedEvent;
import rxsearch.events.SearcherCompletedEvent;
import rxsearch.events.SearcherErrorEvent;
import rxsearch.events.SearcherResultEvent;
import rxsearch.filter.HitProcessor;
import rxsearch.metrics.Measured;
import rxsearch.searcher.Searcher;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.ext.dropwizard.reporters.JmxReporter;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.ResponseTimeHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VertxServer extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxServer.class);

    private final OkHttpClient asyncHttpClient;
    private final List<Searcher> searchers;
    private final List<HitProcessor> processors;

    private SearchRouter searchRouter;
    private HttpServer httpServer;


    public VertxServer() {
        asyncHttpClient = new OkHttpClient();
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder(asyncHttpClient);

        searchers = configurationBuilder.loadConfiguration("rxsearch", "testing", "de").searcher();
        processors = Lists.newArrayList();


        JmxReporter reporter = JmxReporter.forRegistry(SharedMetricRegistries.getOrCreate(Measured.SEARCHER_METRICS))
                .build();
        reporter.start();
    }

    @Override
    public void start(Future<Void> fut) {
        searchRouter = new SearchRouter(searchers, processors, vertx.eventBus());

        httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);

        router.route().handler(LoggerHandler.create());
        router.route().handler(ResponseTimeHandler.create());


        router.route("/search/hits").handler(BodyHandler.create());
        router.route("/search/hits").handler(TimeoutHandler.create());
        router.post("/search/hits").handler(searchRouter);
        router.get("/loaderio-527965f7c4480c76ed72d38029e876c1").handler(rc -> rc.response().end("loaderio-527965f7c4480c76ed72d38029e876c1"));

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
                LOGGER.info("Failed starting server: ", result.cause());
                fut.fail(result.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        LOGGER.info("Stopping server");
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
        Stopwatch stopwatch = Stopwatch.createStarted();

        Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new DropwizardMetricsOptions().setEnabled(true).setJmxEnabled(true)));

        Json.mapper.registerModule(new GuavaModule());
        Json.mapper.registerModule(new KotlinModule());
        Json.prettyMapper.registerModule(new GuavaModule());
        Json.prettyMapper.registerModule(new KotlinModule());

        vertx.eventBus().registerDefaultCodec(SearcherCompletedEvent.class, SearcherCompletedEvent.Codec());
        vertx.eventBus().registerDefaultCodec(SearcherErrorEvent.class, SearcherErrorEvent.Codec());
        vertx.eventBus().registerDefaultCodec(SearcherResultEvent.class, SearcherResultEvent.Codec());
        vertx.eventBus().registerDefaultCodec(SearchFinishedEvent.class, SearchFinishedEvent.Codec());
        vertx.eventBus().registerDefaultCodec(SearchFailedEvent.class, SearchFailedEvent.Codec());
        vertx.eventBus().registerDefaultCodec(SearchStartedEvent.class, SearchStartedEvent.Codec());
        vertx.eventBus().registerDefaultCodec(OfferProcessedEvent.class, OfferProcessedEvent.Codec());


        Future<String> logSearchFuture = Future.future();
        Future<String> loggingFuture = Future.future();
        Future<String> httpFuture = Future.future();

        vertx.deployVerticle(LogSearchVerticle.class.getName(), logSearchFuture.completer());
        vertx.deployVerticle(LoggingVerticle.class.getName(), loggingFuture.completer());
        vertx.deployVerticle(VertxServer.class.getName(), httpFuture.completer());

        CompositeFuture.all(loggingFuture, logSearchFuture, httpFuture).setHandler(ar -> {
            if (ar.succeeded()) {
                stopwatch.stop();
                LOGGER.info("Startup in {}", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            } else {
                stopwatch.stop();
                LOGGER.error("Failed startup ", ar.cause());
                System.exit(-1);
            }
        });


        Runtime.getRuntime().addShutdownHook(new Thread(vertx::close));
    }

}
