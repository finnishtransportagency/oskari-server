package org.oskari.statistics.plugins.unsd;

import fi.nls.oskari.service.ServiceRuntimeException;
import fi.nls.oskari.util.IOHelper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RegionMapper {

    private List<CountryRegion> countries;

    public RegionMapper() {
        countries = readResource().stream()
                .map(row -> parseRow(row))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private static Optional<CountryRegion> parseRow(String row) {
        // FI;FIN;246;Finland
        String[] data = row.split(";");
        if (data.length != 4) {
            return Optional.empty();
        }
        String name = data[3];
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        CountryRegion c = new CountryRegion(data[3]);
        c.addCode(CountryRegion.Type.ISO2, data[0]);
        c.addCode(CountryRegion.Type.ISO3, data[1]);
        c.addCode(CountryRegion.Type.M49, data[2]);
        if (c.isValid()) {
            return Optional.of(c);
        }
        return Optional.empty();
    }

    private List<String> readResource() {
        try {
            return IOHelper.readLines(getClass().getResourceAsStream("/M49codes.csv"));
        } catch (IOException ex) {
            throw new ServiceRuntimeException("Unable to read M49 country mapping", ex);
        }
    }

    public Optional<CountryRegion> find(String anyCode) {
        return countries.stream().filter(c -> c.matches(anyCode)).findFirst();
    }
}