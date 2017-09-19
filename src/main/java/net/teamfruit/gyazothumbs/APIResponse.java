package net.teamfruit.gyazothumbs;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

public class APIResponse {

	private List<ImageBean> images;
	private String message;

	public List<ImageBean> getImages() {
		return this.images;
	}

	public void setImages(final List<ImageBean> images) {
		this.images = images;
	}

	public String getErrorMessage() {
		return this.message;
	}

	public boolean isError() {
		return this.message==null;
	}

	public static class ResponseDeserilizer implements JsonDeserializer<APIResponse> {
		private final Gson gson = new Gson();

		@Override
		public APIResponse deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
			if (json.isJsonArray()) {
				final APIResponse res = new APIResponse();
				final List<ImageBean> list = GyazoThumbs.gson.fromJson(json, new TypeToken<List<ImageBean>>() {
				}.getType());
				res.setImages(list);
				return res;
			} else
				return this.gson.fromJson(json, APIResponse.class);
		}

	}
}
