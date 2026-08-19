package org.janus.shared.domain.base.repository;

import org.janus.shared.domain.base.model.BaseEntity;

import java.util.List;
import java.util.Optional;

public interface GenericRepository <T extends BaseEntity, ID> {

    T save(T entity);

    T insert(T entity);

    int deleteById(ID id);

    int deleteAll();

    int deleteAllById(List<ID> ids);

    Optional<T> findById(ID id);

    boolean existsById(ID id);

}
