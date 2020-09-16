package org.dshaver.covid.domain.epicurve;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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

    @JsonDeserialize(contentAs = BaseEpicurvePoint.class)
    @JsonSerialize(contentAs = BaseEpicurvePoint.class)
    Collection<EpicurvePoint> data;
}
