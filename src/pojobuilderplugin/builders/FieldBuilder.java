package pojobuilderplugin.builders;

import static pojobuilderplugin.util.BuilderPlugInConstants.SPACE;
import static pojobuilderplugin.util.BuilderPlugInConstants.NEW_LINE;
import static pojobuilderplugin.util.BuilderPlugInConstants.SEMI_COLON;

public class FieldBuilder extends BaseBuilder {

	@Override
	public FieldBuilder name(String fieldName) {
		strSourceBuilder.append(fieldName).append(SEMI_COLON).append(NEW_LINE);
		return this;
	}

	@Override
	public FieldBuilder type(String strType) {
		strSourceBuilder.append(strType).append(SPACE);
		return this;
	}

	@Override
	public StringBuilder toSource() {
		return strSourceBuilder;
	}

}
