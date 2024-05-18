package mymb.mymbbchyperledgerfabric.repository;

import mymb.mymbbchyperledgerfabric.entity.Token;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TokenRepository extends MongoRepository<Token, String> {

    Token findByTokenNumber(String tokenNumber);
    List<Token> findByTokenNumberIn(List<String> tokenNumbers);
}
