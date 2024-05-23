package mymb.mymbbchyperledgerfabric.repository;

import mymb.mymbbchyperledgerfabric.entity.Pay;
import mymb.mymbbchyperledgerfabric.entity.PayStatusEnum;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayRepository extends MongoRepository<Pay, String> {

    Pay findByMemberId(String memberId);
    List<Pay> findAllByMemberId(String memberId);
    Pay findByFundingId(String fundingId);
}
