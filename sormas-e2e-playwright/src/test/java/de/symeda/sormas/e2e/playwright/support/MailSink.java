package de.symeda.sormas.e2e.playwright.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The copy's local mail sink (Mailpit API): the app sends every mail there; nothing leaves the host. reset.sh empties it.
 */
public final class MailSink {

	private final String url;
	private final HttpClient http = HttpClient.newHttpClient();

	public MailSink(Copy copy) {
		this.url = copy.mailUrl();
	}

	public String url() {
		return url;
	}

	/** The message list: total, messages[] with ID, From, To, Subject. */
	public JsonObject messages() {
		return get("/api/v1/messages");
	}

	/** One message with its Text and HTML body. */
	public JsonObject message(String id) {
		return get("/api/v1/message/" + id);
	}

	/** Waits up to the timeout for at least n messages; returns the list. */
	public JsonObject awaitMessages(int n, int timeoutMs) throws InterruptedException {
		long end = System.currentTimeMillis() + timeoutMs;
		JsonObject m = messages();
		while (m.get("total").getAsInt() < n && System.currentTimeMillis() < end) {
			Thread.sleep(2000);
			m = messages();
		}
		return m;
	}

	private JsonObject get(String path) {
		try {
			HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(url + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
			if (r.statusCode() != 200) {
				throw new IllegalStateException("mail sink " + path + ": HTTP " + r.statusCode());
			}
			return JsonParser.parseString(r.body()).getAsJsonObject();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}
}
