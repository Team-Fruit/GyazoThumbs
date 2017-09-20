package net.teamfruit.gyazothumbs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.http.HttpEntity;
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
	public Deque<ImageBean> queue = new ArrayDeque<>();
	public File file;
	private int total = 0;
	private AtomicInteger progress = new AtomicInteger();

	public void launch() {
		final BasicThreadFactory factory = new BasicThreadFactory.Builder().namingPattern("GyazoThumbs-download-thread-%d").build();
		this.executor = Executors.newFixedThreadPool(ARGS.getThreadSize(), factory);
		this.file = new File(ARGS.getDir()).getAbsoluteFile();
		this.file.mkdirs();
		Log.LOG.info("GyazoThumbs");
		Log.LOG.info("Directory: "+this.file);
		run();
	}

	public void run() {
		int page = 0;
		while (!this.queue.isEmpty()||this.total==0) {
			final CountDownLatch latch = new CountDownLatch(this.total-this.progress.get()>=100 ? 100 : this.total-this.progress.get());
			ImageBean bean;
			while ((bean = this.queue.poll())!=null) {
				final String url = StringUtils.substring(bean.getThumbUrl(), 0, 30)+"7680/"+StringUtils.substringAfterLast(bean.getThumbUrl(), "/");
				final Future<Void> future = this.executor.submit(new Downloader(url, new File(this.file, StringUtils.substringAfterLast(url, "_")), latch));
			}
			if (page*100>=this.total&&this.total!=0) {
				end();
				break;
			}
			try {
				if (page!=0)
					latch.await();
				final URIBuilder builder = new URIBuilder("https://api.gyazo.com/api/images")
						.addParameter("access_token", ARGS.getToken())
						.addParameter("page", String.valueOf(++page))
						.addParameter("per_page", "100");
				final HttpGet get = new HttpGet(builder.build());
				try (CloseableHttpResponse res = this.client.execute(get)) {
					this.total = Integer.valueOf(res.getFirstHeader("X-Total-Count").getValue());
					final APIResponse api = GyazoThumbs.gson.fromJson(new InputStreamReader(res.getEntity().getContent()), APIResponse.class);
					this.queue.addAll(api.getImages());
				}
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void end() {
		try {
			this.executor.shutdown();
			this.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			this.executor.shutdownNow();
		} catch (final InterruptedException e) {
			Log.LOG.error(e);
		}
	}

	public int getTotal() {
		return this.total;
	}

	public AtomicInteger getProgress() {
		return this.progress;
	}

	public static class Downloader implements Callable<Void> {

		private final String url;
		private final File file;
		private final CountDownLatch latch;

		public Downloader(final String url, final File file, final CountDownLatch latch) {
			this.url = url;
			this.file = file;
			this.latch = latch;
		}

		@Override
		public Void call() throws Exception {
			try {
				if (!this.file.exists()) {
					Log.LOG.info("Downloading "+GyazoThumbs.instance.getProgress().incrementAndGet()+"/"+GyazoThumbs.instance.getTotal()+": "+this.file.getName());
					final HttpGet get = new HttpGet(this.url);
					try (final CloseableHttpResponse res = GyazoThumbs.instance.client.execute(get)) {
						if (res.getStatusLine().getStatusCode()==HttpStatus.SC_OK) {
							final HttpEntity entity = res.getEntity();
							if (entity!=null)
								try (FileOutputStream fos = new FileOutputStream(this.file)) {
									entity.writeTo(fos);
								}
							else
								Log.LOG.warn("Download failed: "+this.url);
						} else
							Log.LOG.warn("Download failed: "+this.url+" "+res.getStatusLine().getStatusCode());
					}
				} else
					Log.LOG.info("Skipped "+GyazoThumbs.instance.getProgress().incrementAndGet()+"/"+GyazoThumbs.instance.getTotal()+": "+this.file.getName());
				return null;
			} finally {
				this.latch.countDown();
			}
		}

	}
}
