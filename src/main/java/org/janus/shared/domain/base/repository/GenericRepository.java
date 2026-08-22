package org.janus.shared.domain.base.repository;

import org.janus.shared.domain.base.model.BaseEntity;

import java.util.List;
import java.util.Optional;

public interface GenericRepository<T extends BaseEntity, ID> {

    T save(T entity);

    List<T> findAll();

    T insert(T entity);

    int deleteById(ID id);

    int deleteById(ID id, long expectedVersion);

    int deleteAll();

    int deleteAllById(List<ID> ids);

    // =========================================================
    // RESTORE & FORCE DELETE
    // =========================================================

    int restoreById(ID id);

    int restoreAllByIds(List<ID> ids);

    int deleteForceById(ID id);

    int deleteAllForceById(List<ID> ids);

    // =========================================================
    // READ
    // =========================================================

    Optional<T> findById(ID id);

    boolean existsById(ID id);
}