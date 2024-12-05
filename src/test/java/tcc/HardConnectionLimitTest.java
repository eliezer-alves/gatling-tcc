package tcc;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.time.Duration;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;

public class HardConnectionLimitTest extends Simulation {

    // Load configuration properties for easy host management
    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = HardConnectionLimitTest.class.getClassLoader().getResourceAsStream("config.properties")) {
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
    String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(baseUrl)
            .userAgentHeader("Hard Connection Limit Test");

    // Cenário simples para testar o limite de conexões
    ChainBuilder simpleRequest = exec(
            http("Teste de Conexão")
                    .get("/") // Endpoint simples
                    .check(status().is(200)) // Verifica se retorna HTTP 200
    );

    ScenarioBuilder connectionTestScenario = scenario("Teste de Limite de Conexões")
            .exec(simpleRequest);

    {
        setUp(
                connectionTestScenario.injectOpen(
                        // Configuração da carga
                        constantUsersPerSec(50).during(Duration.ofSeconds(30)), // 10 usuários/seg por 30s
                        rampUsersPerSec(50).to(1000).during(Duration.ofSeconds(60)), // Escalona de 50 para 1000 usuários/seg em 60s
                        constantUsersPerSec(1000).during(Duration.ofSeconds(120)) // Mantém 1000 usuários/seg por 2 minutos
                )
        ).protocols(httpProtocol);
    }
}