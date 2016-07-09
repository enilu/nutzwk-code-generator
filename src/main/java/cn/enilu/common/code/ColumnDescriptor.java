package cn.enilu.common.code;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import org.nutz.lang.Strings;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColumnDescriptor {
	private static Map<String, Class<?>> typeMapping = new HashMap<String,Class<?>>();

	static {
		typeMapping.put("varchar", String.class);
		typeMapping.put("enum", String.class);
		typeMapping.put("bigint", Long.class);
		typeMapping.put("long", Long.class);
		typeMapping.put("integer", Integer.class);
		typeMapping.put("float", Float.class);
		typeMapping.put("double", Double.class);
		typeMapping.put("int", Integer.class);
		typeMapping.put("timestamp", Integer.class);
		typeMapping.put("datetime", Integer.class);
		typeMapping.put("boolean", boolean.class);
		typeMapping.put("bool", boolean.class);
		typeMapping.put("decimal", BigDecimal.class);
	}

	private static Map<String, Class<?>> validationRules = new HashMap<String,Class<?>>();

	static {
		validationRules.put("pattern",
				javax.validation.constraints.Pattern.class);
		validationRules.put("email", Email.class);
		validationRules.put("url", URL.class);
		validationRules.put("length", Length.class);
		validationRules.put("notnull", NotNull.class);
		validationRules.put("notempty", NotEmpty.class);
		validationRules.put("min", Min.class);
		validationRules.put("max", Max.class);
	}

	private static Map<String, String> labelMapping = new HashMap<String,String>();

	static {
		labelMapping.put("id", "ID");
		labelMapping.put("created_at", "创建时间");
		labelMapping.put("updated_at", "更新时间");
	}

	public static class Validation {
		public final Class<?> klass;
		private final String annotation;

		public Validation(Class<?> klass, String annotation) {
			this.klass = klass;
			this.annotation = annotation;
		}

		public String getAnnotation() {
			return annotation;
		}
	}

	private static Pattern COLUMN_TYPE_PATTERN = Pattern
			.compile("^(\\w+)(?:\\((\\d+)\\))?");
	private static Pattern ENUM_PATTERN = Pattern.compile("enum\\((.+)\\)");

	public String columnName;
	private String label;
	public boolean primary;
	public String dataType;

	public String columnType;
	public int size;

	public boolean nullable;
	private Object defaultValue;
	private String comment;

	private List<String> enumValues = new ArrayList<String>();

	private List<Validation> validations = new ArrayList<Validation>();
	private boolean validationBuilt = false;

	private String queryOperator;

	public List<Validation> getValidations() {
		if (!validationBuilt) {
			if (!containsValidation(NotNull.class)) {
				if (!primary && defaultValue == null && !nullable) {
					Validation validation = new Validation(NotNull.class,
							"@NotNull");
					validations.add(validation);
				}
			}

			validationBuilt = true;
		}
		return validations;
	}

	private boolean containsValidation(Class<?> klass) {
		for (Validation v : validations) {
			if (v.klass == klass) {
				return true;
			}
		}
		return false;
	}

	public boolean hasLabel() {
		return label != null;
	}

	public String getLabel() {
		if (label != null) {
			return label;
		}
		String defaultLabel = labelMapping.get(columnName);
		if (defaultLabel != null) {
			return defaultLabel;
		}
		return columnName;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
		if (comment == null) {
			return;
		}

		extractLabel(comment);
		extractSearchable(comment);

		Pattern validatePattern = Pattern.compile("validate:\\s*(.+)\\s*");
		Matcher m = validatePattern.matcher(comment);
		if (!m.find()) {
			return;
		}
		String validateDef = m.group(1);
		String defs[] = validateDef.split(";");
		// validate: pattern(regexp="^\w+$", message="只能是字母数字组合")
		Pattern defPattern = Pattern.compile("^(\\w+)(?:\\(([^\\)]*)\\))?$");
		for (String def : defs) {
			m = defPattern.matcher(def);
			if (!m.find()) {
				System.err.println("invalid validate def: " + def);
				continue;
			}

			String rule = m.group(1).toLowerCase();
			String params = m.group(2);

			Class<?> ruleClass = validationRules.get(rule);
			if (ruleClass == null) {
				System.err.println("no validation rule for " + def);
				continue;
			}

			StringBuilder code = new StringBuilder("@");
			code.append(ruleClass.getSimpleName());
			if (! Strings.isBlank(params)) {
				code.append("(");
				if (ruleClass == javax.validation.constraints.Pattern.class) {
					params = params.replaceAll("\\\\", "\\\\\\\\");
				}
				// TODO, validate params
				code.append(params);
				code.append(")");
			}
			Validation validation = new Validation(ruleClass, code.toString());
			validations.add(validation);
		}
	}

	private void extractLabel(String comment) {
		Pattern labelPattern = Pattern.compile("label:\\s*([^,;，]+)");
		Matcher m = labelPattern.matcher(comment);
		if (m.find()) {
			this.label = m.group(1);
		}
	}

	public String getQueryOperator() {
		return queryOperator;
	}

	private void extractSearchable(String comment) {
		// searchable: eq
		Pattern queryPattern = Pattern.compile("searchable:\\s*(\\w+)");
		Matcher m = queryPattern.matcher(comment);
		if (m.find()) {
			queryOperator = m.group(1);
		}
	}

	public String getColumnName() {
		return columnName;
	}

	public String getFieldName() {
		return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL,
				columnName);
	}

	public List<String> getEnumValues() {
		return enumValues;
	}

	public void setColumnType(String columnType) {
		Matcher m = ENUM_PATTERN.matcher(columnType);
		if (m.find()) {
			this.columnType = "enum";

			String s = m.group(1);
			for (String v : s.split(",")) {
				v = v.trim().replaceAll("'", "");
				enumValues.add(v);
			}
			return;
		}

		m = COLUMN_TYPE_PATTERN.matcher(columnType);
		if (m.find()) {
			if (m.group(2) != null) {
				this.size = Integer.parseInt(m.group(2));
			}
			this.columnType = m.group(1);
		} else {
			throw new IllegalArgumentException();
		}
	}

	public String getJavaType() {
		if ("tinyint".equalsIgnoreCase(dataType) && size == 1) {
			return boolean.class.getName();
		}
		if ("enum".equalsIgnoreCase(dataType)) {
			return getUpperJavaFieldName();
		}
		Class<?> type = typeMapping.get(dataType);
		if (type != null) {
			return type.getName();
		}

		return String.class.getName();
	}

	public String getSimpleJavaTypeName() {
		return getJavaType().replaceFirst("^.*\\.", "");
	}

	public boolean isEnum() {
		return "enum".equalsIgnoreCase(dataType);
	}

	public boolean isBoolean() {
		return boolean.class.getName().equals(getJavaType());
	}

	public boolean isTimestamp() {
		return "timestamp".equalsIgnoreCase(dataType);
	}

	public String getUpperJavaFieldName() {
		return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL,
				columnName);
	}

	public String getGetterMethodName() {
		if (isBoolean()) {
			return "is" + getUpperJavaFieldName();
		}
		return "get" + getUpperJavaFieldName();
	}

	public String getSetterMethodName() {
		return "set" + getUpperJavaFieldName();
	}

	public String getColumnAnnotation() {
		if (primary) {
//			return "@Id";
			return "@Name\r\n	@Prev(els = {@EL(\"uuid()\")})";
		}
		return "@Column";
	}

	public void setDefaultValue(Object v) {
		this.defaultValue = v;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public String getDefaultValueCode() {
		if (isEnum()) {
			return getSimpleJavaTypeName() + "." + defaultValue;
		}
		if (isBoolean()) {
			if ("1".equals(defaultValue.toString())) {
				return "true";
			} else {
				return "false";
			}
		}
		if (isTimestamp()) {
			if (("0000-00-00 00:00:00".equals(defaultValue) || "CURRENT_TIMESTAMP"
					.equals(defaultValue))) {
				return "DateTime.now()";
			}
		}
		if (defaultValue != null && Long.class.getName().equals(getJavaType())) {
			return defaultValue + "L";
		}
		if (defaultValue != null && BigDecimal.class.getName().equals(getJavaType())) {
			return "new BigDecimal(\"" + defaultValue.toString() + "\")";
		}
		return "\"" + getDefaultValue().toString() + "\"";
	}

	public String getValidationFormClass() {
		List<String> result = Lists.newArrayList();
		for (Validation v : getValidations()) {
			if (v.klass == NotNull.class) {
				result.add("required");
			}
			// TODO
		}
		return Joiner.on(' ').join(result);
	}

}