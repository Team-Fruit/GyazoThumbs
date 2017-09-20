package net.teamfruit.gyazothumbs;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

public class Downloader implements Callable<Void> {

	private final String url;
	private final File file;
	private final CountDownLatch latch;
	private final long timestamp;

	public Downloader(final String url, final File file, final CountDownLatch latch, final long timestamp) {
		this.url = url;
		this.file = file;
		this.latch = latch;
		this.timestamp = timestamp;
	}

	@Override
	public Void call() throws Exception {
		try {
			Log.LOG.info("Downloading "+GyazoThumbs.instance.getProgress().incrementAndGet()+"/"+GyazoThumbs.instance.getTotal()+": "+this.file.getName());
			final HttpGet get = new HttpGet(this.url);
			try (final CloseableHttpResponse res = GyazoThumbs.instance.client.execute(get)) {
				if (res.getStatusLine().getStatusCode()==HttpStatus.SC_OK) {
					final HttpEntity entity = res.getEntity();
					if (entity!=null) {
						try (FileOutputStream fos = new FileOutputStream(this.file)) {
							entity.writeTo(fos);
						}
						this.file.setLastModified(this.timestamp);
					} else
						Log.LOG.warn("Download failed: "+this.url);
				} else
					Log.LOG.warn("Download failed: "+this.url+" "+res.getStatusLine().getStatusCode());
			}
			return null;
		} finally {
			this.latch.countDown();
		}
	}

}
