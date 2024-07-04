package tcc;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class ApiSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:3001")
            .userAgentHeader("Agente do Caos - 2023");

    FeederBuilder<String> usersFeeder = tsv("users-payloads.tsv").circular();
    FeederBuilder<String> buscaFeeder = tsv("termos-busca.tsv").circular();

    ChainBuilder createAndFindPeople = exec(
            feed(usersFeeder),
        //     http("Form").get("/computers/new"),
            pause(1),
            http("criação")
                    .post("/users")
                    .body(StringBody("#{payload}"))
                    .header("Content-Type", "application/json")
                    .check(status().in(201, 422, 400))
                    .check(status().saveAs("httpStatus")))
    .pause(Duration.ofMillis(1), Duration.ofMillis(30))
    .doIf(session -> session.contains("location")).then(
        exec(
                http("consulta")
                        .get("#{location}")
        )
    );
    // if the chain didn't finally succeed, have the user exit the whole scenario


//    ChainBuilder criacaoEConsultausers = scenario("Criação E Talvez Consulta de users")
//            .feed(usersFeeder)
//            .exec(http("criação")
//                    .post("/users")
//                    .body(StringBody("#{payload}"))
//                    .header("Content-Type", "application/json")
//                    .check(status().in(201, 422, 400))
//                    .check(status().saveAs("httpStatus"))
//                    .checkIf(session -> session.getString("httpStatus").equals("201"),
//                            header("Location").saveAs("location")))
//            .pause(Duration.ofMillis(1), Duration.ofMillis(30))
//            .doIf(session -> session.contains("location"))
//            .exec(
//                    http("consulta")
//                            .get("#{location}")
//            );

    ChainBuilder userSearch = exec(
            feed(buscaFeeder),
            http("busca válida")
                    .get("/users/search/#{t}")
                    .check(status().in(200, 299))
    );

    ChainBuilder invalidUserSearch = exec(
            feed(buscaFeeder),
            http("busca válida")
                    .get("/users?t=#{t}")
                    .check(status().in(200, 299))
    );

//    ScenarioBuilder buscaInvalidausers = scenario("Busca Inválida de users")
//            .exec(http("busca inválida")
//                    .get("/users")
//                    .check(status().is(400))
//            );

    ScenarioBuilder getUsers = scenario("Usuários").exec(userSearch);
    ScenarioBuilder createUsers = scenario("Cria usuário").exec(createAndFindPeople);
    ScenarioBuilder searchdUsers = scenario("Busca válida de usuários").exec(userSearch);
    ScenarioBuilder searchInvalidUsers = scenario("Busca inválida de usuários").exec(invalidUserSearch);

    {
        setUp(
                createUsers.injectOpen(
                        constantUsersPerSec(2).during(Duration.ofSeconds(10)),
                        constantUsersPerSec(5).during(Duration.ofSeconds(15)).randomized(),
                        rampUsersPerSec(6).to(600).during(Duration.ofMinutes(3))
                ),
                getUsers.injectOpen(
                        constantUsersPerSec(2).during(Duration.ofSeconds(25)),
                        rampUsersPerSec(6).to(40).during(Duration.ofMinutes(3))
                ),
                searchdUsers.injectOpen(
                        constantUsersPerSec(2).during(Duration.ofSeconds(25)),
                        rampUsersPerSec(6).to(40).during(Duration.ofMinutes(3))
                )


//                criacaoEConsultausers.injectOpen(
//                        constantUsersPerSec(2).during(Duration.ofSeconds(10)),
//                        constantUsersPerSec(5).during(Duration.ofSeconds(15)).randomized(),
//                        rampUsersPerSec(6).to(600).during(Duration.ofMinutes(3))
//                ),
//                getUsers.injectOpen(
//                        constantUsersPerSec(2).during(Duration.ofSeconds(25)),
//                        rampUsersPerSec(6).to(100).during(Duration.ofMinutes(3))
//                ),
//                buscaInvalidausers.injectOpen(
//                        constantUsersPerSec(2).during(Duration.ofSeconds(25)),
//                        rampUsersPerSec(6).to(40).during(Duration.ofMinutes(3))
//                )
        ).protocols(httpProtocol);
    }
}
