package net.teamfruit.gyazothumbs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class GyazoThumbs {

	public static final ArgBean ARGS = new ArgBean();
	public static GyazoThumbs instance;

	public static void main(final String[] args) throws CmdLineException {
		new CmdLineParser(ARGS).parseArgument(args);
		instance = new GyazoThumbs();
	}

	public final HttpClient client = HttpClientBuilder.create().build();
	public final ExecutorService executor;

	private GyazoThumbs() {
		this.executor = Executors.newFixedThreadPool(ARGS.getThreadSize(), r -> {
			final Thread t = new Thread(r, "GyazoThumbs-communication-thread");
			t.setDaemon(true);
			return t;
		});
		launch();
	}

	public void launch() {

	}
}
