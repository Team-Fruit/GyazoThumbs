package net.teamfruit.gyazothumbs;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class ArgBean {

	@Argument(index = 0, metaVar = "token", required = true, usage = "Gyazo API Token")
	private String token;

	@Option(name = "-thread", metaVar = "thread", required = false, usage = "Thread size")
	private int thread = 4;

	@Option(name = "-dir", metaVar = "dir", required = false, usage = "Thumbs Dir")
	private String dir = "thumbs";

	public String getToken() {
		return this.token;
	}

	public int getThreadSize() {
		return this.thread;
	}

	public String getDir() {
		return this.dir;
	}
}
