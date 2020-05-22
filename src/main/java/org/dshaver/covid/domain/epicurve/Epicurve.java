package org.dshaver.covid.domain.epicurve;

import lombok.*;

import java.util.Collection;

/**
 * Target internal representation of a single epicurve
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode
public class Epicurve {
    @NonNull
    String county;
    Collection<EpicurvePoint> data;
}
