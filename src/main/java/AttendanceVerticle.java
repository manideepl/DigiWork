import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import org.bson.Document;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.slf4j.LoggerFactory.getLogger;

public class AttendanceVerticle extends AbstractVerticle {

  Logger logger = getLogger(AttendanceVerticle.class);

  //  Redis
  RedisAPI redisAPI = null;
  MongoClient mongoClient = null;

  @Override
  public void start(Promise<Void> startPromise) {
    Router router = Router.router(vertx);
    ConfigRetriever retriever = ConfigRetriever.create(vertx);


    retriever.getConfig(ar -> {
      if (ar.failed()) {
        logger.error("error retrieving config, cannot do DB/cache stuff");
      } else {
        JsonObject json = ar.result();
        String mongoUri = json.getString("MONGO_ENDPOINT");
        String redisUri = json.getString("REDIS_ENDPOINT");

        initMongo(mongoUri);
        initRedis(redisUri);

      }
    });

    router.get("/addPunch").handler(req -> {
      List<String> punchPair = new ArrayList<>();
      punchPair.add("lastPunch");
//      TODO read from request body
      JsonObject punch = new JsonObject();
      punch.put("remarks", "first punch in ever!");
      punch.put("time", System.currentTimeMillis());
      punch.put("type", "IN");
      punchPair.add(punch.encodePrettily());

      redisAPI.set(punchPair).onSuccess(succ -> {
        logger.debug("succ: " + succ);
        req.json("cached :)");
      }).onFailure(fail -> {
        logger.error("fail: " + fail);
        req.json("couldn't cache :(");
      });

    });


    router.get("/lastPunch").handler(req -> {
      redisAPI.get("lastPunch").onSuccess(succ -> {
        logger.debug("lastPunch: " + succ);
        if (succ != null) req.end(succ.toBuffer());

        req.end("could not get last punch");


      }).onFailure(fail -> {
        logger.error("fail: " + fail);
        req.json(fail);
      });
    });

    router.get("/attendance").handler(req -> {

      List<JsonObject> requests = new ArrayList<>();
      mongoClient.getDatabase("dws2").getCollection("attendance").find().forEach(document -> {
          requests.add(new JsonObject(document.toJson()));
      });

      req.json(requests);

    });


    vertx.createHttpServer().requestHandler(router).listen(8080, ar -> {
      if (ar.succeeded()) {
        logger.debug("server started at localhost:8080");
      } else {
        logger.error("could not start server");
      }
    });
  }

  private void initMongo(String mongoUri) {
    logger.debug("mongo--> " + mongoUri);
    MongoClientSettings settings = MongoClientSettings.builder()
      .applyConnectionString(new ConnectionString(mongoUri))
      .build();

    mongoClient = MongoClients.create(settings);
  }

  private void initRedis(String redisUri) {
    logger.debug("redis--> " + redisUri);

    RedisOptions redisOptions = new RedisOptions();
    redisOptions.setConnectionString(redisUri);

    Redis redis = Redis.createClient(vertx, redisOptions);
    redis.connect().onSuccess(redisSucc -> {
      logger.debug("connected to redis, yay!");
      redisAPI = RedisAPI.api(redisSucc);
    }).onFailure(redisFail -> {
      logger.error("oh snap! you cannot redis this time.");
      logger.error(redisFail.getCause().getMessage());
    });
  }
}
