/*
 * Decompiled with CFR 0.152.
 */
package com.tridium.slottool;

import com.tridium.slottool.Compiler;
import com.tridium.slottool.SlotomaticOptions;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class Slotomatic {
    private final SlotomaticOptions slotomaticOptions = new SlotomaticOptions();
    private static final Logger logger = Logger.getLogger("slotomatic");

    private Slotomatic() {
    }

    public static Slotomatic builder() {
        return new Slotomatic();
    }

    public Slotomatic withModulePath(Path modulePath) {
        this.slotomaticOptions.setModuleRoot(modulePath);
        return this;
    }

    public Slotomatic withPaths(Iterable<Path> paths) {
        this.slotomaticOptions.setPaths(paths);
        return this;
    }

    public Slotomatic compile() {
        return this.compile(true);
    }

    public Slotomatic compile(boolean compile) {
        this.slotomaticOptions.setCompile(compile);
        return this;
    }

    public Slotomatic migrate() {
        return this.migrate(true);
    }

    public Slotomatic migrate(boolean migrate) {
        this.slotomaticOptions.setMigrate(migrate);
        return this;
    }

    public Slotomatic importFromModuleInclude() {
        return this.importFromModuleInclude(true);
    }

    public Slotomatic importFromModuleInclude(boolean importFromModuleInclude) {
        this.slotomaticOptions.setImportFromModuleInclude(importFromModuleInclude);
        return this;
    }

    public Slotomatic sortImports() {
        return this.sortImports(true);
    }

    public Slotomatic sortImports(boolean sortImports) {
        this.slotomaticOptions.setSortImports(sortImports);
        return this;
    }

    public Slotomatic force() {
        return this.force(true);
    }

    public Slotomatic force(boolean force) {
        this.slotomaticOptions.setForceRecompile(force);
        return this;
    }

    public Slotomatic failOnInvalidFile() {
        return this.failOnInvalidFile(true);
    }

    public Slotomatic failOnInvalidFile(boolean failOnInvalidFile) {
        this.slotomaticOptions.setFailOnInvalidFile(failOnInvalidFile);
        return this;
    }

    public Slotomatic withJavaLanguageLevel(String javaLanguageLevel) {
        this.slotomaticOptions.setJavaLanguageLevel(javaLanguageLevel);
        return this;
    }

    public Slotomatic withInputCharset(String charset) {
        this.slotomaticOptions.setInputCharset(charset);
        return this;
    }

    Slotomatic withLineSeparator(String lineSeparator) {
        this.slotomaticOptions.setLineSeparator(lineSeparator);
        return this;
    }

    public Slotomatic strictAnnotationMode() {
        return this.strictAnnotationMode(true);
    }

    public Slotomatic strictAnnotationMode(boolean strictAnnotationMode) {
        this.slotomaticOptions.setStrictAnnotationMode(strictAnnotationMode);
        return this;
    }

    public Slotomatic preserveDate() {
        return this.preserveDate(true);
    }

    public Slotomatic preserveDate(boolean preserveDate) {
        this.slotomaticOptions.setPreserveDate(preserveDate);
        return this;
    }

    public void runSlotomatic() {
        if (this.slotomaticOptions.getPaths().isEmpty()) {
            this.slotomaticOptions.setPaths(Collections.singleton(this.slotomaticOptions.getModuleRoot()));
        }
        Slotomatic.runSlotomatic(this.slotomaticOptions, this.slotomaticOptions.getModuleRoot(), this.slotomaticOptions.getPaths());
    }

    private static void runSlotomatic(SlotomaticOptions options, Path modulePath, Set<Path> paths) {
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("Must specify at least one Path to compile");
        }
        Path moduleIncludePath = modulePath.resolve("module-include.xml");
        Path moduleTestIncludePath = modulePath.resolve("moduleTest-include.xml");
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Compiling " + modulePath.toString());
        }
        Compiler compiler = new Compiler(options, moduleIncludePath, moduleTestIncludePath);
        paths.forEach(compiler::compile);
        Slotomatic.printResults(compiler);
    }

    private static void printResults(Compiler compiler) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(String.format("Compiled %d files, skipped %d files, failed %d files", compiler.getCompileCount(), compiler.getSkipCount(), compiler.getFailCount()));
        }
        if (compiler.getFailCount() > 0) {
            logger.warning("The following files failed to compile: ");
            compiler.getFailedFiles().forEach((file, message) -> logger.warning("  " + file.toAbsolutePath().toString() + ": " + message));
        }
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogHandler(Handler handler) {
        Handler[] handlers;
        for (Handler h : handlers = logger.getHandlers()) {
            logger.removeHandler(h);
        }
        logger.setUseParentHandlers(false);
        handler.setLevel(Optional.ofNullable(logger.getLevel()).orElse(Level.INFO));
        logger.addHandler(handler);
    }

    static {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SlotomaticLogFormatter());
        Slotomatic.setLogHandler(handler);
    }

    public static class SlotomaticLogFormatter
    extends Formatter {
        @Override
        public String format(LogRecord record) {
            String level = record.getLevel().getLocalizedName();
            String message = this.formatMessage(record);
            String throwable = Optional.ofNullable(record.getThrown()).map(thrown -> {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println();
                record.getThrown().printStackTrace(pw);
                pw.close();
                return sw.toString();
            }).orElse("");
            return String.format("[%s] %s %s%n", level, message, throwable);
        }
    }
}

