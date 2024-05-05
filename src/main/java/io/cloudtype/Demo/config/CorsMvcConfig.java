package io.cloudtype.Demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {

        corsRegistry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedOrigins("https://port-0-swr-17xco2nlst8pr67.sel5.cloudtype.app")
                .allowedOrigins("https://teamswr.store")
                .allowedOrigins("http://localhost:5173");
    }
}