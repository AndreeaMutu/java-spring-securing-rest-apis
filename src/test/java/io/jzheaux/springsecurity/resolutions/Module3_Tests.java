package io.jzheaux.springsecurity.resolutions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.Filter;

import java.util.List;
import java.util.UUID;

import static io.jzheaux.springsecurity.resolutions.ReflectionSupport.annotation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc(print= MockMvcPrint.NONE)
@SpringBootTest
public class Module3_Tests {
    @Autowired
    MockMvc mvc;

    @Autowired
    FilterChainProxy springSecurityFilterChain;

    @Autowired(required = false)
    CorsConfigurationSource cors;

    @Test
    public void task_1() {
        // add @CrossOrigin

        CrossOrigin crossOrigin = annotation(CrossOrigin.class, "read");
        assertNotNull(
                "Task 1: Please add the `@CrossOrigin` annotation to the `ResolutionController#read` method",
                crossOrigin);
    }

    @Test
    public void task_2() {
        task_1();
        // cors()

        CorsFilter filter = getFilter(CorsFilter.class);
        assertNotNull(
                "Task 2: It doesn't appear that `cors()` is being called on the `HttpSecurity` object. If it is, make " +
                "sure that `ResolutionsApplication` is extending `WebSecurityConfigurerAdapter` and is overriding `configure(HttpSecurity http)`",
                filter);
    }

    @Test
    public void task_3() throws Exception {
        task_2();
        // global settings

        CorsConfiguration configuration = this.cors.getCorsConfiguration
                (new MockHttpServletRequest("GET", "/" + UUID.randomUUID()));
        assertEquals(
                "Task 3: Make sure that globally you are only allowing `HEAD`",
                1, configuration.getAllowedMethods().size());
        assertEquals(
                "Task 3: Make sure that globally you are only allowing `HEAD`",
                "HEAD", configuration.getAllowedMethods().get(0));
        assertEquals(
                "Task 3: Make sure that globally you are only allowing the `Authorization` header",
                1, configuration.getAllowedHeaders().size());
        assertEquals(
                "Task 3: Make sure that globally you are only allowing the `Authorization` header",
                "Authorization", configuration.getAllowedHeaders().get(0));
        assertEquals(
                "Task 3: Make sure that globally you are only allowing the `http://localhost:4000` origin",
                1, configuration.getAllowedOrigins().size());
        assertEquals(
                "Task 3: Make sure that globally you are only allowing the `http://localhost:4000` origin",
                "http://localhost:4000", configuration.getAllowedOrigins().get(0));

        MvcResult result = this.mvc.perform(options("/resolutions")
            .header("Access-Control-Request-Method", "GET")
            .header("Origin", "http://localhost:4000"))
            .andReturn();

        assertEquals(
                "Task 3: Tried to do an `OPTIONS` pre-flight request from `http://localhost:4000` for `GET /resolutions` failed.",
                200, result.getResponse().getStatus());

        result = this.mvc.perform(options("/resolutions")
                .header("Access-Control-Request-Method", "GET")
                .header("Origin", "http://localhost:5000"))
                .andReturn();

        assertNotEquals(
                "Task 3: Tried to do an `OPTIONS` pre-flight request from `http://localhost:5000` for `GET /resolutions` and it succeeded.",
                200, result.getResponse().getStatus());

        result = this.mvc.perform(options("/" + UUID.randomUUID())
                .header("Access-Control-Request-Method", "GET")
                .header("Origin", "http://localhost:4000"))
                .andReturn();

        assertNotEquals(
                "Task 3: Tried to do an `OPTIONS` pre-flight request from `http://localhost:4000` for a random endpoint, and it succeeded.",
                200, result.getResponse().getStatus());
    }

    @Test
    public void task_4() throws Exception {
        task_3();

        CrossOrigin crossOrigin = annotation(CrossOrigin.class, "read");
        assertEquals(
                "Task 4: So that HTTP Basic works in the browser for this request, set the `allowCredentials` property on the `@CrossOrigin` annotation to `\"true\"`",
                "true", crossOrigin.allowCredentials()
        );

        MvcResult result = this.mvc.perform(options("/resolutions")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Allow-Credentials", "true")
                .header("Origin", "http://localhost:4000"))
                .andReturn();

        assertEquals(
                "Task 4: Tried to do an `OPTIONS` pre-flight request from `http://localhost:4000` for `GET /resolutions` failed.",
                "true", result.getResponse().getHeader("Access-Control-Allow-Credentials"));

        result = this.mvc.perform(options("/" + UUID.randomUUID())
                .header("Access-Control-Request-Method", "HEAD")
                .header("Access-Control-Allow-Credentials", "true")
                .header("Origin", "http://localhost:4000"))
                .andReturn();

        assertNotEquals(
                "Task 4: Tried to do an `OPTIONS` pre-flight request for a random endpoint, asking to send credentials, and it succeeded. " +
                "Make sure that you haven't allowed credentials globally.",
                "true", result.getResponse().getHeader("Access-Control-Allow-Credentials"));
    }

    private <T extends Filter> T getFilter(Class<T> filterClass) {
        List<Filter> filters = this.springSecurityFilterChain.getFilters("/resolutions");
        for (Filter filter : filters) {
            if (filter.getClass() == filterClass) {
                return (T) filter;
            }
        }

        return null;
    }
}
