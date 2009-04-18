package org.codehaus.jackson.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.NumberOutput;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Numeric node that contains 64-bit ("double precision")
 * floating point values simple 32-bit integer values.
 */
public final class DoubleNode
    extends NumericNode
{
    final double _value;

    public DoubleNode(double v) { _value = v; }

    public static DoubleNode valueOf(double v) { return new DoubleNode(v); }

    @Override
        public boolean isFloatingPointNumber() { return true; }

    @Override
        public boolean isDouble() { return true; }

    @Override
    public Number getNumberValue() {
        return Double.valueOf(_value);
    }

    @Override
        public int getIntValue() { return (int) _value; }

    @Override
        public long getLongValue() { return (long) _value; }

    @Override
        public double getDoubleValue() { return _value; }

    @Override
        public BigDecimal getDecimalValue() { return BigDecimal.valueOf(_value); }

    @Override
    public BigInteger getBigIntegerValue() {
        return getDecimalValue().toBigInteger();
    }

    public String getValueAsText() {
        return NumberOutput.toString(_value);
    }

    @Override
    public final void serialize(JsonGenerator jg, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        jg.writeNumber(_value);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) { // final class, can do this
            return false;
        }
        return ((DoubleNode) o)._value == _value;
    }

    @Override
    public int hashCode()
    {
        // same as hashCode Double.class uses
        long l = Double.doubleToLongBits(_value);
        return ((int) l) ^ (int) (l >> 32);

    }
}
