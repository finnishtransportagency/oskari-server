package org.geotools.mif.column;

public class LogicalMIDColumn extends MIDColumn {

    public LogicalMIDColumn(String name) {
        super(name);
    }

    @Override
    public Boolean parse(String str) {
        return Boolean.parseBoolean(str);
    }

    @Override
    public Class<?> getAttributeClass() {
        return Boolean.class;
    }

}
