package org.dshaver.covid.domain.epicurve;

import com.google.common.collect.Multimap;

import java.util.Collection;

public interface Epicurve {
    Multimap<String, EpicurvePoint> getAllEpicurves();

    Collection<EpicurvePoint> getEpicurveForCounty(String county);

    Collection<EpicurvePoint> getStateEpicurve();
}
