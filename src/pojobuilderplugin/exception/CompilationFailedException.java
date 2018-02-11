package pojobuilderplugin.exception;

public class CompilationFailedException extends BuilderPlugInException {
	private static final long serialVersionUID = 1L;

	public CompilationFailedException(String message){
		super(message);
	}
}
