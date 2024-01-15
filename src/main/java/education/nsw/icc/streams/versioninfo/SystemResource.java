package education.nsw.icc.streams.versioninfo;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/system")
public class SystemResource {

  @ConfigProperty(name = "quarkus.application.name")
  String appName;

  @ConfigProperty(name = "quarkus.application.version")
  String appVersion;

  @ConfigProperty(name = "quarkus.profile")
  String profile;

  @GET
  @Path("/info")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, String> getInfo() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("application_name", this.appName);
    map.put("application_version", this.appVersion);
    map.put("profile", this.profile);
    return map;
  }
}
