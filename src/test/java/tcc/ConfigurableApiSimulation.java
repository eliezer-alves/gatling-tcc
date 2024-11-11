package tcc;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.time.Duration;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

public class ConfigurableApiSimulation extends Simulation {

    // Load configuration properties for easy host management
    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = ConfigurableApiSimulation.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return properties;
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return properties;
    }

    Properties config = loadProperties();
    String baseUrl = config.getProperty("base.url", "http://localhost/v1");

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(baseUrl)
            .userAgentHeader("Agente do Caos - 2023");

    FeederBuilder<String> usersFeeder = tsv("users-payload.tsv").circular();
    FeederBuilder<String> buscaFeeder = tsv("termos-busca.tsv").circular();

    ChainBuilder createAndFindPeople = exec(
            feed(usersFeeder),
            http("criação")
                    .post("/users/create")
                    .body(StringBody("#{payload}"))
                    .header("Content-Type", "application/json")
                    .check(status().saveAs("httpStatus"))
                    .check(bodyString().saveAs("responseBody"))
    )
    .pause(Duration.ofMillis(10))
    .doIf(session -> session.getString("httpStatus").equals("201")).then(
        exec(
                http("consulta")
                        .get("/users/#{payload}")
                        .check(status().is(200))
                        .check(bodyString().saveAs("consultaResponse"))
        )
    );

    ChainBuilder userSearch = exec(
            feed(buscaFeeder),
            http("busca válida")
                    .get("/users/search/#{t}")
                    .check(status().is(200))
                    .check(bodyString().saveAs("searchResponse"))
    );

    ChainBuilder invalidUserSearch = exec(
            http("busca inválida")
                    .get("/users")
                    .check(status().is(400))
                    .check(bodyString().saveAs("invalidSearchResponse"))
    );

    ScenarioBuilder getUsers = scenario("Usuários").exec(userSearch);
    ScenarioBuilder createUsers = scenario("Cria usuário").exec(createAndFindPeople);
    ScenarioBuilder searchdUsers = scenario("Busca válida de usuários").exec(userSearch);
    ScenarioBuilder searchInvalidUsers = scenario("Busca inválida de usuários").exec(invalidUserSearch);

    {
        setUp(
                createUsers.injectOpen(
                        constantUsersPerSec(2).during(Duration.ofSeconds(10)),
                        constantUsersPerSec(5).during(Duration.ofSeconds(15)).randomized(),
                        rampUsersPerSec(6).to(60).during(Duration.ofSeconds(30))
                ),
                getUsers.injectOpen(
                        constantUsersPerSec(2).during(Duration.ofSeconds(25)),
                        rampUsersPerSec(6).to(40).during(Duration.ofMinutes(3))
                ),
                searchdUsers.injectOpen(
                        constantUsersPerSec(2).during(Duration.ofSeconds(25)),
                        rampUsersPerSec(6).to(40).during(Duration.ofMinutes(3))
                ),
                searchInvalidUsers.injectOpen(
                        constantUsersPerSec(2).during(Duration.ofSeconds(25)),
                        rampUsersPerSec(6).to(40).during(Duration.ofMinutes(3))
                )
        ).protocols(httpProtocol);
    }
}