package net.teamfruit.gyazothumbs;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GyazoThumbs {

	public static final ArgBean ARGS = new ArgBean();
	public static Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ssZ").registerTypeAdapter(APIResponse.class, new APIResponse.ResponseDeserilizer()).create();
	public static GyazoThumbs instance = new GyazoThumbs();

	public static void main(final String[] args) throws CmdLineException {
		new CmdLineParser(ARGS).parseArgument(args);
		instance.launch();
	}

	public CloseableHttpClient client = HttpClientBuilder.create().build();
	public ExecutorService executor;
	public final Deque<ImageBean> queue = new ArrayDeque<>();
	public final File dir = new File(ARGS.getDir()).getAbsoluteFile();

	private AtomicInteger progress = new AtomicInteger();
	private APIHeader header;

	public void launch() {
		final BasicThreadFactory factory = new BasicThreadFactory.Builder().namingPattern("GyazoThumbs-download-thread-%d").build();
		this.executor = Executors.newFixedThreadPool(ARGS.getThreadSize(), factory);
		this.dir.mkdirs();
		Log.LOG.info("GyazoThumbs");
		Log.LOG.info("Directory: "+this.dir);

		try {
			final URIBuilder builder = new URIBuilder("https://api.gyazo.com/api/images")
					.addParameter("access_token", ARGS.getToken())
					.addParameter("page", String.valueOf(1))
					.addParameter("per_page", "100");
			final HttpGet get = new HttpGet(builder.build());
			try (CloseableHttpResponse res = this.client.execute(get)) {
				this.header = new APIHeader(res);
				final APIResponse api = GyazoThumbs.gson.fromJson(new InputStreamReader(res.getEntity().getContent()), APIResponse.class);
				this.queue.addAll(api.getImages());
			}
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		run();
	}

	public void run() {
		final long start = System.currentTimeMillis();
		int page = 0;
		loop: while (!this.queue.isEmpty()) {
			final int remaining = this.header.getTotalCount()-this.progress.get();
			final CountDownLatch latch = new CountDownLatch(remaining>=100 ? 100 : remaining);
			ImageBean bean;
			while ((bean = this.queue.poll())!=null) {
				final String url = StringUtils.substring(bean.getThumbUrl(), 0, 30)+"7680/"+StringUtils.substringAfterLast(bean.getThumbUrl(), "/");
				final File file = new File(this.dir, StringUtils.substringAfterLast(url, "_"));
				if (!file.exists())
					this.executor.submit(new Downloader(url, file, latch));
				else {
					if (ARGS.isNewer())
						break loop;
					latch.countDown();
					Log.LOG.info("Skipped "+GyazoThumbs.instance.getProgress().incrementAndGet()+"/"+GyazoThumbs.instance.getTotal()+": "+file.getName());
				}
			}

			if (page*100>=this.header.getTotalCount())
				break;

			try {
				if (page!=0)
					latch.await();
				final URIBuilder builder = new URIBuilder("https://api.gyazo.com/api/images")
						.addParameter("access_token", ARGS.getToken())
						.addParameter("page", String.valueOf(++page))
						.addParameter("per_page", "100");
				final HttpGet get = new HttpGet(builder.build());
				try (CloseableHttpResponse res = this.client.execute(get)) {
					final APIResponse api = GyazoThumbs.gson.fromJson(new InputStreamReader(res.getEntity().getContent()), APIResponse.class);
					this.queue.addAll(api.getImages());
				}
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}

		try {
			this.executor.shutdown();
			this.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			Log.LOG.info("Download Complete");
			Log.LOG.info("Total Time: {}", DurationFormatUtils.formatDuration(System.currentTimeMillis()-start, "HH:mm:ss"));
			System.exit(0);
		} catch (final InterruptedException e) {
			Log.LOG.error(e);
		}
	}

	public int getTotal() {
		return this.header.getTotalCount();
	}

	public AtomicInteger getProgress() {
		return this.progress;
	}

}
