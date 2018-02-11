package pojobuilderplugin.builders;

import static pojobuilderplugin.util.BuilderPlugInConstants.PRIVATE;
import static pojobuilderplugin.util.BuilderPlugInConstants.PROTECTED;
import static pojobuilderplugin.util.BuilderPlugInConstants.PUBLIC;
import static pojobuilderplugin.util.BuilderPlugInConstants.SPACE;
import static pojobuilderplugin.util.BuilderPlugInConstants.STATIC;

import java.lang.reflect.Field;

import pojobuilderplugin.wrapper.MyField;
public abstract class BaseBuilder implements IBuilder {
	protected StringBuilder strSourceBuilder;
	public BaseBuilder() {
		strSourceBuilder = new StringBuilder();
	}
	public void addSpace() {
		strSourceBuilder.append(SPACE);
	}

	public BaseBuilder setPublic() {
		strSourceBuilder.append(PUBLIC);
		return this;
	}

	public BaseBuilder setProtected() {
		strSourceBuilder.append(PROTECTED);
		return this;
	}

	public BaseBuilder setPrivate() {
		strSourceBuilder.append(PRIVATE);
		return this;
	}

	public BaseBuilder setStatic() {
		strSourceBuilder.append(STATIC);
		return this;
	}
	
	public IBuilder returns(String returnType) {
		throw new UnsupportedOperationException("Operation not supported.");
	}

	public IBuilder name(String name) {
		throw new UnsupportedOperationException("Operation not supported.");
	}

	public IBuilder name(String name, MyField mf) {
		throw new UnsupportedOperationException("Operation not supported.");
	}

	public IBuilder name(String name, Class<?> clazz) {
		throw new UnsupportedOperationException("Operation not supported.");
	}
	
	public IBuilder code(String strSourceCode) {
		throw new UnsupportedOperationException("Operation not supported.");
	}

	public IBuilder fields(Field[] fieldNames) {
		throw new UnsupportedOperationException("Operation not supported.");
	}
	
	public IBuilder mutators(MyField[] fieldNames) {
		throw new UnsupportedOperationException("Operation not supported.");
	}
	
	public IBuilder fields(MyField[] fieldNames) {
		throw new UnsupportedOperationException("Operation not supported.");
	}

	public IBuilder type(String strType) {
		throw new UnsupportedOperationException("Operation not supported.");
	}
	
	public IBuilder buildMethod(Class<?> clazz) {
		throw new UnsupportedOperationException("Operation not supported.");
	}
}
