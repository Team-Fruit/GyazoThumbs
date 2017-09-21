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
import org.apache.http.HttpStatus;
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
	public File dir;

	private AtomicInteger progress = new AtomicInteger();
	private APIHeader header;

	public void launch() {
		final BasicThreadFactory factory = new BasicThreadFactory.Builder().namingPattern("GyazoThumbs-download-thread-%d").build();
		this.executor = Executors.newFixedThreadPool(ARGS.getThreadSize(), factory);
		this.dir = new File(ARGS.getDir()).getAbsoluteFile();
		this.dir.mkdirs();
		Log.LOG.info("GyazoThumbs");
		Log.LOG.info("Directory: "+this.dir);

		try {
			final URIBuilder builder = new URIBuilder("https://api.gyazo.com/api/images")
					.addParameter("access_token", ARGS.getToken())
					.addParameter("page", String.valueOf(1))
					.addParameter("per_page", String.valueOf(ARGS.getMax()<100 ? ARGS.getMax() : 100));
			final HttpGet get = new HttpGet(builder.build());
			try (CloseableHttpResponse res = this.client.execute(get)) {
				this.header = new APIHeader(res);
				if (res.getStatusLine().getStatusCode()==HttpStatus.SC_OK)
					try (InputStreamReader isr = new InputStreamReader(res.getEntity().getContent())) {
						final APIResponse api = GyazoThumbs.gson.fromJson(isr, APIResponse.class);
						this.queue.addAll(api.getImages());
					}
				else
					throw new RuntimeException(String.format("%s %s", res.getStatusLine().getStatusCode(), res.getStatusLine().getReasonPhrase()));
			}
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		if (ARGS.isPro()&&this.header.getUserType().equals("lite"))
			Log.LOG.error("Your account is not a pro!");
		else {
			Log.LOG.info("Total-Count: {}", this.header.getTotalCount());
			Log.LOG.info("User-Type: {}", this.header.getUserType());

			run();
		}
	}

	public void run() {
		final long start = System.currentTimeMillis();
		int page = 1;
		int count = 0;
		loop: while (!this.queue.isEmpty()) {
			final int remaining = this.header.getTotalCount()-this.progress.get();
			final CountDownLatch latch = new CountDownLatch(remaining>=100 ? 100 : remaining);
			ImageBean bean;
			while ((bean = this.queue.poll())!=null) {
				if (count>=ARGS.getMax())
					break loop;

				final String url = ARGS.isPro() ? bean.getUrl() : StringUtils.substring(bean.getThumbUrl(), 0, 30)+"7680/"+StringUtils.substringAfterLast(bean.getThumbUrl(), "/");
				final File file = new File(this.dir, StringUtils.substringAfterLast(url, ARGS.isPro() ? "/" : "_"));
				if (file.exists()&&ARGS.isNew())
					break loop;
				this.executor.submit(new Downloader(url, file, latch, bean.getCreatedAt().getTime()));
				count++;
			}

			if (page*100>=this.header.getTotalCount()||count>=ARGS.getMax())
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
					if (res.getStatusLine().getStatusCode()==HttpStatus.SC_OK)
						try (InputStreamReader isr = new InputStreamReader(res.getEntity().getContent())) {
							final APIResponse api = GyazoThumbs.gson.fromJson(isr, APIResponse.class);
							this.queue.addAll(api.getImages());
						}
					else
						throw new RuntimeException(String.format("%s %s", res.getStatusLine().getStatusCode(), res.getStatusLine().getReasonPhrase()));
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
