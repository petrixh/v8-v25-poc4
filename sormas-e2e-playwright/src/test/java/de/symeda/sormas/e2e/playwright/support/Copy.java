package de.symeda.sormas.e2e.playwright.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * One running app copy (env/up.sh): its port, compose project and kind. Containers are named from the project
 * (&lt;project&gt;-app, &lt;project&gt;-db, &lt;project&gt;-mail).
 */
public final class Copy {

	final int port;
	final String project;
	final AppCopy.Config config;
	final boolean passwordCheck;
	/** The last class on this copy changed data the app holds in memory: the next reset restarts the app. */
	volatile boolean dirty;

	Copy(int port, String project, AppCopy.Config config, boolean passwordCheck) {
		this.port = port;
		this.project = project;
		this.config = config;
		this.passwordCheck = passwordCheck;
	}

	/** scheme://host:port of the app; the UI is under /sormas-ui. */
	public String base() {
		return "http://localhost:" + port;
	}

	public String project() {
		return project;
	}

	boolean is(AppCopy kind) {
		return config == kind.value() && passwordCheck == kind.passwordCheck();
	}

	/** The mail sink's web UI and API (Mailpit) of this copy, from its published port. */
	public String mailUrl() {
		String out = run(List.of("docker", "port", project + "-mail", "8025/tcp")).trim().split("\n")[0];
		return "http://" + out.replace("0.0.0.0", "127.0.0.1");
	}

	/**
	 * One SQL query on this copy's database (sormas_db), as the postgres superuser in the database container; returns
	 * the rows as psql prints them unaligned (fields separated by |, one row per line), trimmed.
	 */
	public String sql(String query) {
		return run(List.of("docker", "exec", project + "-db", "psql", "-U", "postgres", "-d", "sormas_db", "-v", "ON_ERROR_STOP=1", "-qAt", "-c", query))
			.trim();
	}

	/** Runs a command inside the app container and returns its output. */
	public String appExec(String... command) {
		List<String> cmd = new java.util.ArrayList<>(List.of("docker", "exec", project + "-app"));
		cmd.addAll(List.of(command));
		return run(cmd);
	}

	static String run(List<String> command) {
		try {
			Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
			String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			if (!p.waitFor(60, TimeUnit.SECONDS) || p.exitValue() != 0) {
				throw new IllegalStateException(command.get(0) + " " + command.get(1) + " failed: " + out.trim());
			}
			return out;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String toString() {
		return project + " (" + config + (passwordCheck ? ", password check on" : "") + ", port " + port + ")";
	}
}
