package org.dshaver.covid.dao;

import org.dshaver.covid.domain.RawDataV1;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by xpdf64 on 2020-04-27.
 */
public interface RawDataRepository extends MongoRepository<RawDataV1, String> {

}
