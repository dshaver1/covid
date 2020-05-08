package org.dshaver.covid.dao;

import org.dshaver.covid.domain.RawDataV2;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by xpdf64 on 2020-04-27.
 */
public interface RawDataRepository2 extends MongoRepository<RawDataV2, String> {

}
