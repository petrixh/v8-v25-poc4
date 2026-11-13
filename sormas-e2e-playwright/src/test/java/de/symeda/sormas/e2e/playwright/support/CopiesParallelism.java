package de.symeda.sormas.e2e.playwright.support;

import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.support.hierarchical.DefaultParallelExecutionConfigurationStrategy;
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfiguration;
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfigurationStrategy;

/** As many test classes at once as the run has app copies (ports in e2e.ports). */
public class CopiesParallelism implements ParallelExecutionConfigurationStrategy {

	@Override
	public ParallelExecutionConfiguration createConfiguration(ConfigurationParameters parameters) {
		int copies = AppCopyPool.copyPorts().size();
		return DefaultParallelExecutionConfigurationStrategy.FIXED.createConfiguration(new ConfigurationParameters() {

			@Override
			public java.util.Optional<String> get(String key) {
				return "fixed.parallelism".equals(key) ? java.util.Optional.of(String.valueOf(copies)) : parameters.get(key);
			}

			@Override
			public java.util.Optional<Boolean> getBoolean(String key) {
				return parameters.getBoolean(key);
			}

			@SuppressWarnings("deprecation")
			@Override
			public int size() {
				return parameters.size();
			}

			@Override
			public java.util.Set<String> keySet() {
				return parameters.keySet();
			}
		});
	}
}
