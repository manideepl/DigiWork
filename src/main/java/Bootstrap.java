import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

public class Bootstrap extends AbstractVerticle {
  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(AttendanceVerticle.class.getCanonicalName());
  }
}
