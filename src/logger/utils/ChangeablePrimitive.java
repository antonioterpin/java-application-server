package logger.utils;

/**
 * Parameterized class which provides a way to overcome the pointer reference of java.<br>
 * Java is neither pass-by-value nor pass-by-reference, this way allows to create a way to modify any Object.
 * @author Antonio Terpin
 * @param <T>
 */
public class ChangeablePrimitive<T> {
	T value;
	public ChangeablePrimitive(T value) {
		this.value = value;
	}
	public T getValue() { return value; }
	public void setValue(T value) { this.value = value; }
}
