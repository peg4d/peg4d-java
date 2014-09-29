package org.peg4d.query;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ArgumentsParser {
	protected Optional<OptionListener> defaultListener = Optional.empty();

	protected Optional<Option> helpOption = Optional.empty();

	/**
	 * contains recognized options
	 */
	protected final List<Option> optionList = new ArrayList<>();

	/**
	 * key is option name (short name or long name)
	 */
	protected final Map<String, Option> optionMap = new HashMap<>();

	protected final Set<Option> requireOptionSet = new HashSet<>();

	private int maxSizeOfUsage;

	/**
	 * 
	 * @param shortName
	 * may be null. not start with '--'
	 * @param longName
	 * may be null. not start with '-'
	 * @param hasArg
	 * @param description
	 * may be null
	 * @param listener
	 * may be null
	 * @return
	 * this
	 * @throws IllegalArgumentException
	 */
	public ArgumentsParser addOption(String shortName, String longName, 
	                                 boolean hasArg, String description, 
	                                 OptionListener listener) throws IllegalArgumentException {
		return this.addOption(shortName, longName, hasArg, description, false, listener);
	}

	/**
	 * 
	 * @param shortName
	 * may be null. not start with '--'
	 * @param longName
	 * may be null. not start with '-'
	 * @param hasArg
	 * @param description
	 * may be null
	 * @param require
	 * @param listener
	 * may be null
	 * @return
	 * this
	 * @throws IllegalArgumentException
	 */
	public ArgumentsParser addOption(String shortName, String longName, 
            boolean hasArg, String description, boolean require,
            OptionListener listener) throws IllegalArgumentException {
		return this.addOption(shortName, longName, hasArg, description, require, listener, false);
	}

	protected ArgumentsParser addOption(String shortName, String longName, 
            boolean hasArg, String description, boolean require,
            OptionListener listener, boolean asHelp) throws IllegalArgumentException {
		// check short name format
		Optional<String> actualShortName = Optional.empty();
		if(shortName != null) {
			if(shortName.startsWith("--")) {
				throw new IllegalArgumentException("illegal short name: " + shortName);
			}
			actualShortName = Optional.of(shortName.startsWith("-") ? shortName : "-" + shortName);
			if(this.optionMap.containsKey(actualShortName.get())) {
				throw new IllegalArgumentException("found duplicated option: " + actualShortName);
			}
		}

		// check long name format
		Optional<String> actualLongName = Optional.empty();
		if(longName != null) {
			if(!longName.startsWith("--") && longName.startsWith("-")) {
				throw new IllegalArgumentException("illegal long name: " + longName);
			}
			actualLongName = Optional.of(longName.startsWith("--") ? longName : "--" + longName);
			if(this.optionMap.containsKey(actualLongName.get())) {
				throw new IllegalArgumentException("found duplicated option: " + actualLongName);
			}
		}

		// add option
		Option option = new Option(actualShortName, actualLongName, hasArg, 
				Optional.ofNullable(listener), Optional.ofNullable(description), require);
		this.optionList.add(option);
		actualShortName.ifPresent(key -> this.optionMap.put(key, option));
		actualLongName.ifPresent(key -> this.optionMap.put(key, option));

		int usageSize = option.getUsage().length();
		if(this.maxSizeOfUsage < usageSize) {
			this.maxSizeOfUsage = usageSize;
		}
		if(option.isRequiredOption()) {
			this.requireOptionSet.add(option);
		}
		if(asHelp) {
			this.helpOption = Optional.of(option);
		}
		return this;
	}

	/**
	 * 
	 * @param listener
	 * may be null
	 * @return
	 */
	public ArgumentsParser addDefaultAction(OptionListener listener) {
		this.defaultListener = Optional.ofNullable(listener);
		return this;
	}

	public ArgumentsParser addHelp(String shortName, String longName, boolean hasArg, 
             String description, OptionListener listener) throws IllegalArgumentException {
		return this.addOption(shortName, longName, hasArg, description, false, listener, true);
	}

	public void parseAndInvokeAction(String[] args) throws IllegalArgumentException {
		final List<Pair<Optional<String>, Optional<OptionListener>>> pairList = new ArrayList<>();

		// parse arguments
		final int size = args.length;

		if(size == 0) {
			this.defaultListener.ifPresent(a -> a.invoke(Optional.empty()));
			return;
		}

		final Set<Option> foundOptionSet = new HashSet<>();
		for(int i = 0; i < size; i++) {
			String optionSymbol = args[i];
			Option option = this.optionMap.get(optionSymbol);
			if(option == null) {
				throw new IllegalArgumentException("illegal option: " + optionSymbol);
			}
			if(foundOptionSet.contains(option)) {
				throw new IllegalArgumentException("duplicated option: " + optionSymbol);
			}
			Optional<String> arg = Optional.empty();
			if(option.requireArg()) {
				if(++i < size && !args[i].startsWith("-")) {
					arg = Optional.of(args[i]);
				}
				else {
					throw new IllegalArgumentException("require argument: " + optionSymbol);
				}
			}
			pairList.add(new Pair<Optional<String>, Optional<OptionListener>>(arg, option.getListener()));
			foundOptionSet.add(option);

			// invoke help action if present
			if(this.helpOption.isPresent() && this.helpOption.get().equals(option)) {
				final Optional<String> helpArg = arg;
				option.getListener().ifPresent(a -> a.invoke(helpArg));
			}
		}

		// check require option
		for(Option option : this.requireOptionSet) {
			if(!foundOptionSet.contains(option)) {
				throw new IllegalArgumentException("require option: " + option.getUsage());
			}
		}

		// invoke action
		for(final Pair<Optional<String>, Optional<OptionListener>> pair : pairList) {
			pair.getRight().ifPresent(a -> a.invoke(pair.getLeft()));
		}
	}

	public void printHelpBeforeExit(PrintStream stream, int status) {
		StringBuilder sBuilder = new StringBuilder();
		for(int i = 0; i < this.maxSizeOfUsage; i++) {
			sBuilder.append(' ');
		}
		String spaces = sBuilder.toString();

		// format help message
		sBuilder = new StringBuilder();
		sBuilder.append("Options:");
		sBuilder.append(System.lineSeparator());
		for(Option option : this.optionList) {
			final int size = option.getUsage().length();
			sBuilder.append("    ");
			sBuilder.append(option.getUsage());
			for(int i = 0; i < this.maxSizeOfUsage - size; i++) {
				sBuilder.append(' ');
			}
			Optional<String> desc = option.getDescription();
			if(desc.isPresent()) {
				String[] descs = desc.get().split(System.lineSeparator());
				for(int i = 0; i < descs.length; i++) {
					if(i > 0) {
						sBuilder.append(System.lineSeparator());
						sBuilder.append(spaces);
					}
					sBuilder.append("    ");
					sBuilder.append(descs[i]);
				}
			}
			sBuilder.append(System.lineSeparator());
		}
		stream.print(sBuilder.toString());
		System.exit(status);
	}

	protected static class Option {
		protected final Optional<String> shortName;
		protected final Optional<String> longName;
		protected final boolean hasArg;
		protected final Optional<OptionListener> listener;
		protected final Optional<String> description;
		protected final boolean require;

		protected final String usage;

		/**
		 * 
		 * @param shortName
		 * starts with '-'
		 * @param longName
		 * starts with '--'
		 * @param hasArg
		 * @param listener
		 * not null
		 * @param description
		 * not null
		 *  @param require
		 */
		public Option(Optional<String> shortName, Optional<String> longName, boolean hasArg, 
		              Optional<OptionListener> listener, Optional<String> description, boolean require) {
			this.shortName = shortName;
			this.longName = longName;
			this.hasArg = hasArg;
			this.listener = listener;
			this.description = description;
			this.require = require;

			final StringBuilder sBuilder = new StringBuilder();
			this.shortName.ifPresent(n -> sBuilder.append(n));
			if(this.shortName.isPresent() && this.longName.isPresent()) {
				sBuilder.append(" | ");
			}
			this.longName.ifPresent(n -> sBuilder.append(n));
			if(this.hasArg) {
				sBuilder.append(" <arg>");
			}
			this.usage = sBuilder.toString();
		}

		public Optional<String> getShortName() {
			return this.shortName;
		}

		public Optional<String> getLongName() {
			return this.longName;
		}

		public boolean requireArg() {
			return this.hasArg;
		}

		public Optional<OptionListener> getListener() {
			return this.listener;
		}

		public Optional<String> getDescription() {
			return this.description;
		}

		public boolean isRequiredOption() {
			return this.require;
		}

		public String getUsage() {
			return this.usage;
		}
	}

	@FunctionalInterface
	protected static interface OptionListener {
		public void invoke(Optional<String> arg);
	}
}
