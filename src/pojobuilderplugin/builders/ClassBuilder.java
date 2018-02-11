package pojobuilderplugin.builders;

import static pojobuilderplugin.util.BuilderPlugInConstants.JAVA_CLASS_KEYWORD;
import static pojobuilderplugin.util.BuilderPlugInConstants.NEW_LINE;
import static pojobuilderplugin.util.BuilderPlugInConstants.PARENTHESIS_CLOSE;
import static pojobuilderplugin.util.BuilderPlugInConstants.PARENTHESIS_OPEN;
import static pojobuilderplugin.util.BuilderPlugInConstants.SPACE;
import pojobuilderplugin.util.BuilderPlugInUtil;
import pojobuilderplugin.wrapper.MyField;

public class ClassBuilder extends BaseBuilder {

	private String className;

	@Override
	public ClassBuilder name(String name) {
		BuilderPlugInUtil.errorIfNullOrBlank(name, "Class name");
		this.className = name;
		strSourceBuilder.append(JAVA_CLASS_KEYWORD).append(name).append(SPACE);
		strSourceBuilder.append(PARENTHESIS_OPEN).append(NEW_LINE);
		return this;
	}

	@Override
	public ClassBuilder fields(MyField[] fieldNames) {
		BuilderPlugInUtil.errorIfNull(fieldNames, "Field names");
		for (MyField mf : fieldNames) {
			if(mf != null) {
			StringBuilder strBuilder = new FieldBuilder().setPrivate().type(mf.getType()).name(mf.getActualName())
					.toSource();
			strSourceBuilder.append(strBuilder);
			}
		}

		return this;
	}

	@Override
	public IBuilder mutators(MyField[] fieldNames) {
		BuilderPlugInUtil.errorIfNull(fieldNames, "Field names");
		for (MyField mf : fieldNames) {
			if(mf != null) {
			StringBuilder strMethodCode = new MethodBuilder().setPublic().returns(className)
					.name(mf.getCanonicalName(), mf).toSource();
			strSourceBuilder.append(strMethodCode);
			}
		}
		return this;
	}

	@Override
	public IBuilder buildMethod(@SuppressWarnings("rawtypes") Class clazz) {

		StringBuilder strBuildMethod = new MethodBuilder().setPublic().returns(clazz.getSimpleName()).name("build")
				.code("return new " + clazz.getSimpleName() + "(this);").toSource();
		strSourceBuilder.append(strBuildMethod);
		return this;
	}

	@Override
	public StringBuilder toSource() {
		strSourceBuilder.append(NEW_LINE).append(PARENTHESIS_CLOSE);
		return strSourceBuilder;
	}
	
	@Override
	public String toString() {
		return strSourceBuilder.toString();
	}

}
