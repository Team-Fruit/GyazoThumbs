package net.teamfruit.gyazothumbs;

import org.apache.http.HttpResponse;

public class APIHeader {

	private final int totalCount;
	private final int currentPage;
	private final int perPage;
	private final String userType;

	public APIHeader(final HttpResponse res) {
		this.totalCount = Integer.parseInt(res.getFirstHeader("x-total-count").getValue());
		this.currentPage = Integer.parseInt(res.getFirstHeader("x-current-page").getValue());
		this.perPage = Integer.parseInt(res.getFirstHeader("x-per-page").getValue());
		this.userType = res.getFirstHeader("x-user-type").getValue();
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
