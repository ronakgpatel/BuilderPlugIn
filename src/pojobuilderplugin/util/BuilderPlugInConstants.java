package pojobuilderplugin.util;

import java.lang.reflect.Field;

public class BuilderPlugInConstants {

	public static final String BLANK_STR = "";
	public static final String NO_PREFIX = BLANK_STR;
	public static final String SPACE = " ";
	public static final String SEMI_COLON = ";";
	public static final String EQUAL = "=";
	public static final String PARENTHESIS_CLOSE = "}";
	public static final char PARENTHESIS_CLOSE_CHAR = PARENTHESIS_CLOSE.charAt(0);
	public static final String PARENTHESIS_OPEN = "{";
	public static final char PARENTHESIS_OPEN_CHAR = PARENTHESIS_OPEN.charAt(0);
	public static final String BRACKET_CLOSE = ")";
	public static final String BRACKET_OPEN = "(";
	public static final String BUILDER_CLASS_METHOD_NAME_PREFIX = " with";
	public static final String PUBLIC = "public ";
	public static final String PROTECTED = "protected ";
	public static final String PRIVATE = "private ";
	public static final String NEW_LINE = "\n";
	public static final String FIELD_NAME_PREFIX = "my";
	public static final String PACKAGE_KEYWORD = "package";
	public static final String BUILDER = "Builder";
	public static final String JAVA_CLASS_KEYWORD = "class ";
	public static final String STATIC = "static ";
	public static final String DOT = ".";

	// this function removes the provided prefix from field value and then
	// canonicalize the field name
	// for.e.g myValue -> value( remove 'my' and lowercase 'v')
	public static final Function<Field, String> FunctionToRemovePrefix = new Function<Field, String>() {
		
		@Override
		public String apply(Field f1) {

			if (BuilderPlugInUtil.isNull(FIELD_NAME_PREFIX))
				return f1.getName();
			else {
				String strValue = f1.getName().replaceFirst(FIELD_NAME_PREFIX, "");
				char[] name = strValue.toCharArray();
				if ((int) name[0] < 96) {
					// if first character is caps(since the prefix is stripped),
					// lower case it
					name[0] = (char) (((int) name[0]) + 32);
				}
				return new String(name);
			}

		
		}
	};

}
