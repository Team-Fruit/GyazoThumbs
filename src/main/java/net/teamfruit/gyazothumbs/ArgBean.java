package net.teamfruit.gyazothumbs;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class ArgBean {

	@Argument(index = 0, metaVar = "token", required = true, usage = "Gyazo API Token")
	private String token;

	@Argument(index = 1, metaVar = "max", required = false, usage = "Max Download Count")
	private int max = Integer.MAX_VALUE;

	@Option(name = "-thread", metaVar = "thread", required = false, usage = "Thread size")
	private int thread = 4;

	@Option(name = "-dir", metaVar = "dir", required = false, usage = "Thumbs Dir")
	private String dir = "thumbs";

	@Option(name = "-newer", metaVar = "newer", required = false, usage = "boolean")
	private boolean newer = false;

	public String getToken() {
		return this.token;
	}

	public int getMax() {
		return this.max;
	}

	public int getThreadSize() {
		return this.thread;
	}

	public String getDir() {
		return this.dir;
	}

	public boolean isNewer() {
		return this.newer;
	}
}
