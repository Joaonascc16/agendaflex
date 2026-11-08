package br.com.agendaflex.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** O relógio é um bean para que os testes possam fixar a "data de hoje". */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
