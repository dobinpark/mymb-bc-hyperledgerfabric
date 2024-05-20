package mymb.mymbbchyperledgerfabric.repository;

import mymb.mymbbchyperledgerfabric.entity.Pay;
import mymb.mymbbchyperledgerfabric.entity.PayStatusEnum;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PayRepository extends MongoRepository<Pay, String> {

    Pay findByMemberId(String memberId);
    List<Pay> findAllByMemberId(String memberId);
}
