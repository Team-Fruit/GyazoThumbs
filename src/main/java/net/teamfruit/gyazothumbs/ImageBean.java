package net.teamfruit.gyazothumbs;

import java.sql.Date;

public class ImageBean {

	private String image_id;
	private String permalink_url;
	private String thumb_url;
	private String url;
	private String type;
	private boolean star;
	private Date created_at;

	public String getImageId() {
		return this.image_id;
	}

	public String getPermalinkUrl() {
		return this.permalink_url;
	}

	public String getThumbUrl() {
		return this.thumb_url;
	}

	public String getUrl() {
		return this.url;
	}

	public String getType() {
		return this.type;
	}

	public boolean isStar() {
		return this.star;
	}

	public Date getCreatedAt() {
		return this.created_at;
	}

}
