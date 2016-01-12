package edu.ucla.astro.irlab.io;


public class Parameter {
	Object value;
	String stringValue;
	
	public Parameter(Object val, String strVal) {
		value = val;
		stringValue = strVal;
	}
	
	public Object getValue() {
		return value;
	}
	public String getString() {
		return stringValue;
	}
	
	public String toString() {
		return stringValue;
	}
}
