package com.tissue.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseCleanup {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @SuppressWarnings("unchecked")
    public void execute() {
        entityManager.flush();
        entityManager
                .createNativeQuery("SET session_replication_role = 'replica'")
                .executeUpdate();

        List<String> tableNames = entityManager
                .createNativeQuery(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'")
                .getResultList();

        for (String tableName : tableNames) {
            entityManager
                    .createNativeQuery("TRUNCATE TABLE \"" + tableName + "\" CASCADE")
                    .executeUpdate();
        }

        entityManager
                .createNativeQuery("SET session_replication_role = 'origin'")
                .executeUpdate();
    }
}
