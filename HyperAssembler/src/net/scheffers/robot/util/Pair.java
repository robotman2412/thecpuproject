package net.scheffers.robot.util;

import javafx.beans.NamedArg;

import java.io.Serializable;
import java.util.Objects;

public class Pair<K, V> implements Serializable {

	private final K key;

	public K getKey() { return key; }

	private final V value;

	public V getValue() { return value; }

	public Pair(@NamedArg("key") K key, @NamedArg("value") V value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public String toString() {
		return key + "=" + value;
	}

	@Override
	public int hashCode() {
		// name's hashCode is multiplied by an arbitrary prime number (13)
		// in order to make sure there is a difference in the hashCode between
		// these two parameters:
		//  name: a  value: aa
		//  name: aa value: a
		return key.hashCode() * 13 + (value == null ? 0 : value.hashCode());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o instanceof Pair) {
			Pair<?,?> pair = (Pair<?,?>) o;
			if (!Objects.equals(key, pair.key)) return false;
			return Objects.equals(value, pair.value);
		}
		return false;
	}
}
