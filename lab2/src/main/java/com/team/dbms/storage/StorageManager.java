package com.team.dbms.storage;

import com.team.dbms.core.Database;

public interface StorageManager {
    Database load();
    void save(Database database);
}
