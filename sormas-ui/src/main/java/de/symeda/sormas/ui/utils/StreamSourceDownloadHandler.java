package de.symeda.sormas.ui.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * A download with a file name and MIME type whose content comes from a {@link StreamSource}; replaces the Vaadin 8
 * {@code StreamResource} (with cache time 0) that SORMAS handed to a {@code FileDownloader}. Use it as the href of a
 * download {@code Anchor}, e.g. through {@link DownloadUtil#createDownloadLink}.
 * <p>
 * Like the Vaadin 8 {@code FileDownloader}, the stream is requested with the session locked and written without the
 * lock; the UI of the download is the current UI meanwhile, so a stream source may show a notification with
 * {@link NotificationHelper#showAsync}.
 */
public class StreamSourceDownloadHandler implements DownloadHandler {

	private static final long serialVersionUID = 1L;

	/**
	 * Was {@code com.vaadin.server.StreamResource.StreamSource}: returns the content, or {@code null} if there is none.
	 */
	@FunctionalInterface
	public interface StreamSource extends Serializable {

		InputStream getStream();
	}

	private final StreamSource streamSource;
	private final String fileName;
	private String mimeType;
	private boolean inline;

	public StreamSourceDownloadHandler(StreamSource streamSource, String fileName) {
		this(streamSource, fileName, null);
	}

	public StreamSourceDownloadHandler(StreamSource streamSource, String fileName, String mimeType) {
		this.streamSource = streamSource;
		this.fileName = fileName;
		this.mimeType = mimeType;
	}

	public StreamSource getStreamSource() {
		return streamSource;
	}

	public String getFileName() {
		return fileName;
	}

	public String getMIMEType() {
		return mimeType;
	}

	public void setMIMEType(String mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * Shows the content in the browser (e.g. in an {@code IFrame}, was a Vaadin 8 {@code BrowserFrame}) instead of
	 * downloading it as a file.
	 */
	public StreamSourceDownloadHandler inline() {
		this.inline = true;
		return this;
	}

	@Override
	public void handleDownloadRequest(DownloadEvent event) throws IOException {

		UI previousUi = UI.getCurrent();
		UI.setCurrent(event.getUI());
		try {
			InputStream stream;
			VaadinSession session = event.getSession();
			session.lock();
			try {
				stream = streamSource.getStream();
			} finally {
				session.unlock();
			}

			if (stream == null) {
				// Vaadin 8 answered "not found" when a resource had no stream
				event.getResponse().setStatus(HttpStatusCode.NOT_FOUND.getCode());
				return;
			}

			if (inline) {
				event.inline(fileName);
			} else {
				event.setFileName(fileName);
			}
			if (mimeType != null) {
				event.setContentType(mimeType);
			}
			// was setCacheTime(0)
			event.getResponse().setHeader("Cache-Control", "no-cache");

			try (InputStream in = stream; OutputStream out = event.getOutputStream()) {
				in.transferTo(out);
			}
		} finally {
			UI.setCurrent(previousUi);
		}
	}

	@Override
	public String getUrlPostfix() {
		return fileName;
	}
}
