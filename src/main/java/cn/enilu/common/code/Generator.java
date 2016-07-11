package cn.enilu.common.code;

import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.apache.commons.cli.*;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.impl.NutDao;
import org.nutz.dao.sql.Sql;
import org.nutz.dao.sql.SqlCallback;
import org.nutz.ioc.Ioc;
import org.nutz.ioc.impl.NutIoc;
import org.nutz.ioc.loader.json.JsonLoader;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class Generator {
	private final Map<String, TableDescriptor> tables;
	private final TableDescriptor table;

	public Generator(Map<String, TableDescriptor> tables, TableDescriptor table) {
		this.tables = tables;
		this.table = table;
	}

	public void generate(String packageName, String templatePath, File file,
						 boolean force) throws IOException {
		if (file.exists() && !force) {
			System.out.println("file " + file + " exists, skipped");
			return;
		}

		String code = generateCode(packageName, templatePath);
		file.getParentFile().mkdirs();

		Files.write(code.getBytes(Charsets.UTF_8), file);

	}

	public String generateCode(String packageName, String templatePath)
			throws IOException {
		VelocityContext context = new VelocityContext();
		context.put("table", table);
		context.put("packageName", packageName);
		StringWriter writer = new StringWriter();

		URL url = Resources.getResource(templatePath);
		String template = Resources.toString(url, Charsets.UTF_8);

		VelocityEngine engine = new VelocityEngine();
		engine.setProperty("runtime.references.strict", false);
		engine.init();
		engine.evaluate(context, writer, "generator", template);
		return writer.toString();

	}
	//-p export -c /generator.xml entity
	public static void main(String[] args) throws Exception {

		String configPath = "/code/code.json";
		Ioc ioc = new NutIoc(new JsonLoader(configPath));
		DataSource ds = ioc.get(DataSource.class);
		Dao dao = new NutDao(ds);

		Pattern includePattern = Pattern.compile(".*");
		Pattern excludePattern = null;
		String basePackageName = "cn.wizzer.modules";
		String controllerPackageName = "controllers";
		String servicePackageName = "services";
		String modelPackageName = "models";

		String outputDir = "src/main/java";
		boolean force = false;
		String baseUri = "/";
		String types[] = { "all" };

		Options options = new Options();
		options.addOption("c", "config", true, "spring datasource config file(classpath)");
		options.addOption("i", "include", true, "include table pattern");
		options.addOption("x", "exclude", true, "exclude table pattern");
		options.addOption("p", "package", true, "base package name,default:cn.wizzer.modules");
		options.addOption("ctr", "package", true, "controller base package name,default:cn.wizzer.modules");
		options.addOption("mod", "package", true, "model base package name,default:cn.wizzer.modules");
		options.addOption("sev", "package", true, "service base package name,default:cn.wizzer.modules");
		options.addOption("o", "output", true, "output directory, default is "
				+ outputDir);
		options.addOption("u", "base-uri", true,
				"base uri prefix, default is /");
		options.addOption("f", "force", false,
				"force generate file even if file exists");
		options.addOption("h", "help", false, "show help message");
		CommandLineParser parser = new GnuParser();
		try {
			CommandLine commandLine = parser.parse(options, args);
			if (commandLine.hasOption("c")) {
				configPath = commandLine.getOptionValue("c");
			}
			if (commandLine.hasOption("i")) {
				includePattern = Pattern.compile(commandLine
						.getOptionValue("i"), Pattern.CASE_INSENSITIVE);
			}
			if (commandLine.hasOption("x")) {
				excludePattern = Pattern.compile(
						commandLine.getOptionValue("x"),
						Pattern.CASE_INSENSITIVE);
			}
			if (commandLine.hasOption("p")) {
				basePackageName = commandLine.getOptionValue("p");
			}

			if (commandLine.hasOption("ctr")) {
				controllerPackageName = commandLine.getOptionValue("ctr");
			}
			if (commandLine.hasOption("sev")) {
				servicePackageName = commandLine.getOptionValue("sev");
			}
			if (commandLine.hasOption("mod")) {
				modelPackageName = commandLine.getOptionValue("mod");
			}

			if (commandLine.hasOption("o")) {
				outputDir = commandLine.getOptionValue("o");
			}
			if (commandLine.hasOption("u")) {
				baseUri = commandLine.getOptionValue("u");
			}
			force = commandLine.hasOption("f");
			if (commandLine.hasOption("h")) {
				usage(options);
			}
			String[] extraArgs = commandLine.getArgs();
			if (extraArgs.length > 0) {
				types = extraArgs;
			}
		} catch (Exception e) {
			usage(options);
		}

		Map<String, TableDescriptor> tables = TableDescLoader.loadTables(configPath, basePackageName,
				baseUri,servicePackageName,modelPackageName);

		for (Map.Entry<String, TableDescriptor> entry : tables.entrySet()) {
			String tableName = entry.getKey();
			if (excludePattern != null) {
				if (excludePattern.matcher(tableName).find()) {
					System.out.println("skip " + tableName);
					continue;
				}
			}
			if (includePattern != null) {
				if (!includePattern.matcher(tableName).find()) {
					System.out.println("skip " + tableName);
					continue;
				}
			}

			TableDescriptor table = entry.getValue();

			System.out.println("generate " + tableName + " ...");
			Generator generator = new Generator(tables, table);
			Map<String,String> typeMap = new HashMap<String,String>();
			typeMap.put("model",modelPackageName);
			typeMap.put("service",servicePackageName);
			typeMap.put("controller",controllerPackageName);

			for (String type : new String[] { "model", "service",
					"controller", "view" }) {
				if (!isTypeMatch(types, type)) {
					continue;
				}
				if (type.equals("view")) {
					generateViews(force, table, generator);
				} else {
					String packageName = basePackageName + "." + typeMap.get(type);
					String templatePath = "code/" + type + ".vm";

					String packagePath = packageName.replace('.', '/');
					String className = table.getEntityClassName();
					if (!"model".equals(type)) {
						className = className
								+ CaseFormat.LOWER_UNDERSCORE.to(
								CaseFormat.UPPER_CAMEL, type);
					}
					File file = new File(outputDir, packagePath + "/"
							+ className + ".java");

					generator.generate(packageName, templatePath, file, force);
				}
			}
		}

		System.out.println("done!");
	}

	private static boolean isTypeMatch(String[] types, String type) {
		for (String t : types) {
			if (t.equalsIgnoreCase(type) || "all".equalsIgnoreCase(t)) {
				return true;
			}
		}
		return false;
	}

	private static void generateViews(boolean force, TableDescriptor table,
									  Generator generator) throws IOException {
		for (String view : new String[] { "index", "add", "edit", "show" }) {
			String templatePath = "code/view/" + view + ".html.vm";
			File file = new File("src/main/webapp/WEB-INF/views"
					+ table.getUriPrefix() + "/" + view
					+ ".html");
			generator.generate(null, templatePath, file, force);
		}
	}



	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(
				"Main [options] [all|entity|service|controller|view]", options);
		System.exit(1);
	}

}
