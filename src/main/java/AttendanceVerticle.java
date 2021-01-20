import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.impl.RedisClient;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class AttendanceVerticle extends AbstractVerticle {

  Logger logger = getLogger(AttendanceVerticle.class);

  RedisOptions redisOptions = new RedisOptions();

  RedisAPI redisAPI = null;

  @Override
  public void start(Promise<Void> startPromise) {
    Router router = Router.router(vertx);
    ConfigRetriever retriever = ConfigRetriever.create(vertx);
    redisOptions.setConnectionString("redis-13603.c80.us-east-1-2.ec2.cloud.redislabs.com:13603");

    Redis redis = Redis.createClient(vertx, redisOptions);
    redis.connect().onSuccess(ar -> {
      System.out.println(ar instanceof RedisAPI);
      System.out.println(ar instanceof RedisClient);

      redisAPI = RedisAPI.api(ar);
    });

    retriever.getConfig(ar -> {
      if (ar.failed()) {
        logger.error("error retrieving config, cannot do DB stuff");
      } else {
        JsonObject json = ar.result();
        String mongoUri = json.getString("CONN_STRING");
        logger.debug("uri: " + mongoUri);
      }
    });

    router.get("/attendance").handler(req -> {
      redisAPI.get("last_punch").onSuccess(value -> {
          logger.debug(value.toString());
          req.json("Yay");
      });

    });


    vertx.createHttpServer().requestHandler(router).listen(8080, ar -> {
      if (ar.succeeded()) {
        logger.debug("server started at localhost:8080");
      } else {
        logger.error("could not start server");
      }
    });
  }
}
