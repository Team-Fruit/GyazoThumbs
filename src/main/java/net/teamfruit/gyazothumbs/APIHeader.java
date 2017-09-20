package net.teamfruit.gyazothumbs;

import org.apache.http.HttpResponse;

public class APIHeader {

	private final int totalCount;
	private final int currentPage;
	private final int perPage;
	private final String userType;

	public APIHeader(final HttpResponse res) {
		this.totalCount = Integer.parseInt(res.getFirstHeader("X-Total-Count").getValue());
		this.currentPage = Integer.parseInt(res.getFirstHeader("X-Current-Page").getValue());
		this.perPage = Integer.parseInt(res.getFirstHeader("X-Per-Page").getValue());
		this.userType = res.getFirstHeader("X-User-Type").getValue();
	}

	public int getTotalCount() {
		return this.totalCount;
	}

	public int getCurrentPage() {
		return this.currentPage;
	}

	public int getPerPage() {
		return this.perPage;
	}

	public String getUserType() {
		return this.userType;
	}

}
