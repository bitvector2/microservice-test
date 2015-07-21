package org.bitvector.test;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RESTServer extends AbstractVerticle {
    private Logger logger;
    private Cluster cluster;
    private Session session;

    @Override
    public void start() {
        logger = LoggerFactory.getLogger("org.bitvector.test.RESTServer");//

        // Connect to Database
        vertx.executeBlocking(future -> {
            cluster = Cluster.builder()
                    .addContactPoint(System.getProperty("org.bitvector.test.db-node"))
                    .build();
            session = cluster.connect();
            future.complete();
        }, res -> {
            if (res.failed()) {
                logger.error("Shit Broke!");
            }
        });

        // Start REST Collection
        Product productColl = new Product(session);

        // Start HTTP Router
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.get("/products/:productID").handler(productColl::handleGetProduct);
        router.put("/products/:productID").handler(productColl::handleAddProduct);
        router.get("/products").handler(productColl::handleListProducts);

        // Start HTTP Listener
        vertx.createHttpServer().requestHandler(router::accept).listen(
                Integer.parseInt(System.getProperty("org.bitvector.test.listen-port")),
                System.getProperty("org.bitvector.test.listen-address")
        );

        logger.info("Started a RESTServer...");
    }

    @Override
    public void stop() {
        session.closeAsync();
        cluster.closeAsync();
        logger.info("Stopped a RESTServer...");
    }

}

