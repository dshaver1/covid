package org.dshaver.covid.domain.epicurve;

import com.google.common.collect.Multimap;
import org.dshaver.covid.domain.Identifiable;

import java.util.Collection;

public interface EpicurveDto extends Identifiable {
    Multimap<String, EpicurvePoint> getAllEpicurves();

    Collection<EpicurvePoint> getEpicurveForCounty(String county);

    Collection<EpicurvePoint> getStateEpicurve();
}
