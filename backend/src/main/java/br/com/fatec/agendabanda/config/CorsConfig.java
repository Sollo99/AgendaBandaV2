package br.com.fatec.agendabanda.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public FilterRegistrationBean<CorsFilter> corsAntesDaAutenticacao() {
        var config=new CorsConfiguration();
        config.setAllowedOrigins(java.util.Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(java.util.List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setAllowCredentials(true);
        var source=new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**",config);
        var registro=new FilterRegistrationBean<>(new CorsFilter(source));
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE+10);
        return registro;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(java.util.Arrays.stream(allowedOrigins.split(",")).map(String::trim).toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
