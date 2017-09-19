package net.teamfruit.gyazothumbs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Deque;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
	public Deque<ImageBean> queue = new ConcurrentLinkedDeque<>();
	public File file;

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
		int total = 0;
		int page = 0;
		while (!this.queue.isEmpty()||total==0) {
			ImageBean bean;
			while ((bean = this.queue.poll())!=null) {
				final String url = StringUtils.substring(bean.getThumbUrl(), 0, 30)+"7680/"+StringUtils.substringAfterLast(bean.getThumbUrl(), "/");
				final Future<Void> future = this.executor.submit(new Downloader(url, new File(this.file, StringUtils.substringAfterLast(url, "_"))));
			}
			if (page*100>=total&&total!=0)
				break;
			try {
				final URIBuilder builder = new URIBuilder("https://api.gyazo.com/api/images")
						.addParameter("access_token", ARGS.getToken())
						.addParameter("page", String.valueOf(++page))
						.addParameter("per_page", "100");
				final HttpGet get = new HttpGet(builder.build());
				try (CloseableHttpResponse res = this.client.execute(get)) {
					total = Integer.valueOf(res.getFirstHeader("X-Total-Count").getValue());
					final APIResponse api = GyazoThumbs.gson.fromJson(new InputStreamReader(res.getEntity().getContent()), APIResponse.class);
					this.queue.addAll(api.getImages());
				}
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}

		}
	}

	public static class Downloader implements Callable<Void> {

		private final String url;
		private final File file;

		public Downloader(final String url, final File file) {
			this.url = url;
			this.file = file;
		}

		@Override
		public Void call() throws Exception {
			Log.LOG.info("Downloading: "+this.file.getName());
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
			return null;
		}

	}
}
