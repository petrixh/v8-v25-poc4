package de.symeda.sormas.e2e.playwright.support;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The app copies of one test run: one slot per port of {@code e2e.ports}, each holding at most one copy started with
 * the app repository's env scripts (env/up.sh --no-build, env/reset.sh, env/down.sh; env/README.md).
 * <p>
 * A test class acquires a copy of its kind and gets it in the seed state: an idle copy of that kind is reset (restart
 * reset: database from the seed snapshot, fresh app JVM, mail sink emptied); else a new copy is started on a free port
 * (up.sh leaves it in the seed state); else an idle copy of another kind is removed and a copy of the wanted kind
 * started in its place. With every slot busy the class waits. Copies are removed at the end of the run unless
 * {@code e2e.keepCopies} is true.
 */
public final class AppCopyPool implements AutoCloseable {

	private static final Duration SCRIPT_TIMEOUT = Duration.ofMinutes(20);

	private final Path envDir;
	private final Path logDir;
	private final String projectPrefix;
	private final boolean keep;
	private final boolean reuse;
	private final List<Slot> slots = new ArrayList<>();

	private static final class Slot {

		final int port;
		Copy copy;
		boolean busy;

		Slot(int port) {
			this.port = port;
		}
	}

	AppCopyPool(Path envDir, Path logDir, String projectPrefix, List<Integer> ports, boolean keep, boolean reuse) {
		this.envDir = envDir;
		this.logDir = logDir;
		this.projectPrefix = projectPrefix;
		this.keep = keep;
		this.reuse = reuse;
		ports.forEach(p -> slots.add(new Slot(p)));
	}

	static AppCopyPool fromSystemProperties() {
		Path envDir = Path.of(System.getProperty("e2e.envDir", "../env")).toAbsolutePath().normalize();
		if (!Files.isRegularFile(envDir.resolve("up.sh"))) {
			throw new IllegalStateException("e2e.envDir has no up.sh: " + envDir);
		}
		Path out = Path.of(System.getProperty("e2e.out", "target/e2e")).toAbsolutePath();
		return new AppCopyPool(
			envDir,
			out.resolve("copies"),
			System.getProperty("e2e.project", "sormas-e2e"),
			ports(System.getProperty("e2e.ports", "18720-18721")),
			Boolean.parseBoolean(System.getProperty("e2e.keepCopies", "false")),
			Boolean.parseBoolean(System.getProperty("e2e.reuseCopies", "false")));
	}

	/** "18720-18724" or "18720,18722" */
	static List<Integer> ports(String spec) {
		List<Integer> out = new ArrayList<>();
		for (String part : spec.split(",")) {
			String[] r = part.trim().split("-");
			int a = Integer.parseInt(r[0].trim()), b = r.length > 1 ? Integer.parseInt(r[1].trim()) : a;
			for (int p = a; p <= b; p++) {
				out.add(p);
			}
		}
		return out;
	}

	public int size() {
		return slots.size();
	}

	/** A copy of the given kind in the seed state, for one test class. Release it with {@link #release(Copy)}. */
	public Copy acquire(AppCopy kind) throws InterruptedException {
		while (true) {
			Slot slot;
			String action;
			synchronized (this) {
				slot = find(s -> !s.busy && s.copy != null && s.copy.is(kind));
				action = "reset";
				if (slot == null && reuse) {
					// a kept copy of this kind from an earlier run (e2e.reuseCopies)
					slot = find(s -> !s.busy && s.copy == null && running(candidate(s, kind)) && sameKind(candidate(s, kind)));
					action = "up";
				}
				if (slot == null && reuse) {
					// keep kept copies of other kinds for the classes that need them
					slot = find(s -> !s.busy && s.copy == null && !running(candidate(s, kind)));
					action = "up";
				}
				if (slot == null) {
					slot = find(s -> !s.busy && s.copy == null);
					action = "up";
				}
				if (slot == null) {
					slot = find(s -> !s.busy);
					action = "swap";
				}
				if (slot == null) {
					wait();
					continue;
				}
				slot.busy = true;
			}
			try {
				if (action.equals("reset")) {
					script("reset.sh", slot.copy);
				} else {
					if (slot.copy != null) {
						script("down.sh", slot.copy);
						slot.copy = null;
					}
					Copy c = candidate(slot, kind);
					if (reuse && running(c) && sameKind(c)) {
						// test development: a copy kept from an earlier run (e2e.keepCopies) of the same kind
						script("reset.sh", c);
					} else {
						// down.sh first: a copy left over by an interrupted run would make up.sh refuse to start
						script("down.sh", c);
						script("up.sh", c, "--no-build");
					}
					slot.copy = c;
				}
				return slot.copy;
			} catch (RuntimeException e) {
				synchronized (this) {
					if (slot.copy != null) {
						try {
							script("down.sh", slot.copy);
						} catch (RuntimeException ignored) {
							// reported by the first failure
						}
					}
					slot.copy = null;
					slot.busy = false;
					notifyAll();
				}
				throw e;
			}
		}
	}

	private Copy candidate(Slot s, AppCopy kind) {
		return new Copy(s.port, projectPrefix + "-" + s.port, kind.value(), kind.passwordCheck());
	}

	public synchronized void release(Copy copy) {
		for (Slot s : slots) {
			if (s.copy == copy) {
				s.busy = false;
			}
		}
		notifyAll();
	}

	private Slot find(java.util.function.Predicate<Slot> p) {
		return slots.stream().filter(p).findFirst().orElse(null);
	}

	/**
	 * Whether a running copy is of the copy's kind, from the sormas.properties up.sh wrote for it (later lines win): C2
	 * sets country.locale=de-DE, the password check sets skipDefaultPasswordCheck=false.
	 */
	private boolean sameKind(Copy c) {
		String stateDir = System.getenv().getOrDefault("APP_ENV_STATE_DIR", envDir.resolve(".state").toString());
		Path props = Path.of(stateDir, "copies", c.project, "sormas.properties");
		try {
			String locale = "", skip = "true";
			for (String line : Files.readAllLines(props)) {
				String l = line.trim();
				if (l.startsWith("country.locale=")) {
					locale = l.substring("country.locale=".length()).trim();
				} else if (l.startsWith("skipDefaultPasswordCheck=")) {
					skip = l.substring("skipDefaultPasswordCheck=".length()).trim();
				}
			}
			AppCopy.Config config = locale.startsWith("de") ? AppCopy.Config.C2 : AppCopy.Config.C1;
			return config == c.config && "false".equals(skip) == c.passwordCheck;
		} catch (IOException e) {
			return false;
		}
	}

	private static boolean running(Copy c) {
		try {
			return Copy.run(List.of("docker", "inspect", "-f", "{{.State.Running}}", c.project + "-app")).trim().equals("true");
		} catch (RuntimeException e) {
			return false;
		}
	}

	/** Runs env/&lt;name&gt; for the copy; its output goes to target/e2e/copies/&lt;project&gt;.log. */
	private void script(String name, Copy c, String... args) {
		List<String> cmd = new ArrayList<>(List.of("bash", envDir.resolve(name).toString()));
		cmd.addAll(List.of(args));
		ProcessBuilder pb = new ProcessBuilder(cmd);
		Map<String, String> env = pb.environment();
		env.put("APP_ENV_PORT", String.valueOf(c.port));
		env.put("APP_ENV_PROJECT", c.project);
		env.remove("APP_ENV_CONTAINER");
		env.put("APP_ENV_CONFIG", c.config.name());
		env.put("APP_ENV_PWCHECK", c.passwordCheck ? "on" : "off");
		env.put("APP_ENV_RESET_MODE", "restart");
		try {
			Files.createDirectories(logDir);
			File log = logDir.resolve(c.project + ".log").toFile();
			pb.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log));
			Instant t0 = Instant.now();
			Process p = pb.start();
			if (!p.waitFor(SCRIPT_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
				p.destroyForcibly();
				throw new IllegalStateException(name + " timed out for " + c + " (log: " + log + ")");
			}
			if (p.exitValue() != 0) {
				throw new IllegalStateException(name + " failed for " + c + " (exit " + p.exitValue() + ", log: " + log + ")");
			}
			System.out.printf("[e2e] %s %s: %d s%n", name, c, Duration.between(t0, Instant.now()).toSeconds());
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	/** End of the run: removes every copy (env/down.sh), unless e2e.keepCopies is true. */
	@Override
	public synchronized void close() {
		for (Slot s : slots) {
			if (s.copy != null) {
				if (keep) {
					System.out.println("[e2e] kept " + s.copy + ": remove it with env/down.sh");
				} else {
					try {
						script("down.sh", s.copy);
					} catch (RuntimeException e) {
						System.out.println("[e2e] " + e.getMessage());
					}
				}
				s.copy = null;
			}
		}
	}
}
