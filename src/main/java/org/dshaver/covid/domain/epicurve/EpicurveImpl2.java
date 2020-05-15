package org.dshaver.covid.domain.epicurve;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class EpicurveImpl2 implements Epicurve {
    /**
     * Index the datapoints by county.
     */
    @JsonProperty("map")
    private Map<String, TreeSet<EpicurvePoint>> map = new HashMap<>();

    public EpicurveImpl2() {
    }

    public EpicurveImpl2(Collection<EpicurvePointImpl2> points) {
        points.forEach(point -> {
            TreeSet<EpicurvePoint> set = map.computeIfAbsent(point.getCounty(), k -> new TreeSet<>());
            set.add(point);
        });
    }

    public EpicurveImpl2(Map<String, TreeSet<EpicurvePoint>> map) {
        this.map = map;
    }

    @Override
    public Multimap<String, EpicurvePoint> getAllEpicurves() {
        Multimap<String, EpicurvePoint> tempMap = TreeMultimap.create();
        map.forEach(tempMap::putAll);
        return tempMap;
    }

    @Override
    public Collection<EpicurvePoint> getEpicurveForCounty(String county) {
        return map.get(county);
    }

    @Override
    public Collection<EpicurvePoint> getStateEpicurve() {
        return map.get("Georgia");
    }
}
