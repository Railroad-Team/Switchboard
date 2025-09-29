package dev.railroadide.switchboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.railroadide.logger.Logger;
import dev.railroadide.logger.LoggerManager;
import dev.railroadide.switchboard.json.LocalDateTimeTypeAdapter;
import dev.railroadide.switchboard.json.OptionalTypeAdapterFactory;
import dev.railroadide.switchboard.routing.Router;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.json.JavalinGson;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.ext.java7.PathArgumentType;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Switchboard {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
            .registerTypeAdapterFactory(new OptionalTypeAdapterFactory())
            .setPrettyPrinting()
            .create();
    public static final Logger LOGGER = LoggerManager.registerLogger(LoggerManager.create("Switchboard").build());
    public static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static void main(String[] args) {
        LoggerManager.init();

        ArgumentParser parser = ArgumentParsers.newFor("Switchboard")
                .build()
                .defaultHelp(true)
                .description("A metadata web-service for Railroad IDE.");

        parser.addArgument("-p", "--port")
                .type(Integer.class)
                .setDefault(7000)
                .help("Port to run the web server on (default: 7000)");

        parser.addArgument("-parchmentclonepath", "--parchmentclonepath")
                .type(new PathArgumentType().verifyExists().verifyIsDirectory().verifyCanRead().verifyCanWrite())
                .setDefault(Path.of("parchment"))
                .help("Path to clone the Parchment repository to (default: system temp directory)");

        parser.addArgument("-cacheloc", "--cacheloc")
                .type(new PathArgumentType().verifyExists().verifyIsDirectory().verifyCanRead().verifyCanWrite())
                .setDefault(Path.of(".cache"))
                .help("Path to store cached files (default: .cache)");

        Namespace namespace = parser.parseArgsOrFail(args);
        Environment.load(namespace);

        Javalin app = Javalin.create(config -> {
                    config.http.defaultContentType = ContentType.JSON;
                    config.jsonMapper(new JavalinGson(GSON, true));
                    config.useVirtualThreads = true;
                    config.showJavalinBanner = false;
                })
                .start(Environment.getPort());
        LOGGER.info("Started Switchboard on port {}", Environment.getPort());

        new Router(app).initialize();
        Switchboard.LOGGER.info("Finished initializing router.");
    }
}
