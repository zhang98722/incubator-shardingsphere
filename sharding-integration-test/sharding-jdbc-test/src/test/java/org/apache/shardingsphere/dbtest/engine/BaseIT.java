/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.dbtest.engine;

import com.google.common.base.Joiner;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.shardingsphere.dbtest.cases.assertion.IntegrateTestCasesLoader;
import org.apache.shardingsphere.dbtest.env.DatabaseTypeEnvironment;
import org.apache.shardingsphere.dbtest.env.EnvironmentPath;
import org.apache.shardingsphere.dbtest.env.IntegrateTestEnvironment;
import org.apache.shardingsphere.dbtest.env.datasource.DataSourceUtil;
import org.apache.shardingsphere.dbtest.env.schema.SchemaEnvironmentManager;
import org.apache.shardingsphere.shardingjdbc.api.yaml.YamlMasterSlaveDataSourceFactory;
import org.apache.shardingsphere.shardingjdbc.api.yaml.YamlShardingDataSourceFactory;
import org.apache.shardingsphere.shardingjdbc.jdbc.core.datasource.ShardingDataSource;
import org.apache.shardingsphere.spi.database.DataSourceMetaData;
import org.apache.shardingsphere.spi.database.DatabaseType;
import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.sql.DataSource;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

@RunWith(Parameterized.class)
@Getter(AccessLevel.PROTECTED)
public abstract class BaseIT {
    
    private static IntegrateTestEnvironment integrateTestEnvironment = IntegrateTestEnvironment.getInstance();
    
    private static IntegrateTestCasesLoader integrateTestCasesLoader = IntegrateTestCasesLoader.getInstance();
    
    private final String shardingRuleType;
    
    private final DatabaseTypeEnvironment databaseTypeEnvironment;
    
    private final DataSource dataSource;
    
    private Map<String, DataSource> dataSourceMap;
    
    private Map<String, DataSource> instanceDataSourceMap;
    
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
    
    public BaseIT(final String shardingRuleType, final DatabaseTypeEnvironment databaseTypeEnvironment) throws IOException, JAXBException, SQLException {
        this.shardingRuleType = shardingRuleType;
        this.databaseTypeEnvironment = databaseTypeEnvironment;
        if (databaseTypeEnvironment.isEnabled()) {
            dataSourceMap = createDataSourceMap(shardingRuleType);
            dataSource = createDataSource(dataSourceMap);
            instanceDataSourceMap = createInstanceDataSourceMap();
        } else {
            dataSourceMap = null;
            dataSource = null;
            instanceDataSourceMap = null;
        }
    }
    
    protected final String getExpectedDataFile(final String path, final String shardingRuleType, final DatabaseType databaseType, final String expectedDataFile) {
        if (null == expectedDataFile) {
            return null;
        }
        String prefix = path.substring(0, path.lastIndexOf(File.separator));
        String result = Joiner.on("/").join(prefix, "dataset", shardingRuleType, databaseType.getName().toLowerCase(), expectedDataFile);
        if (new File(result).exists()) {
            return result;
        }
        result = Joiner.on("/").join(prefix, "dataset", shardingRuleType, expectedDataFile);
        if (new File(result).exists()) {
            return result;
        }
        return Joiner.on("/").join(prefix, "dataset", expectedDataFile);
    }
    
    private Map<String, DataSource> createDataSourceMap(final String shardingRuleType) throws IOException, JAXBException {
        Collection<String> dataSourceNames = SchemaEnvironmentManager.getDataSourceNames(shardingRuleType);
        Map<String, DataSource> result = new HashMap<>(dataSourceNames.size(), 1);
        for (String each : dataSourceNames) {
            result.put(each, DataSourceUtil.createDataSource(databaseTypeEnvironment.getDatabaseType(), each));
        }
        return result;
    }
    
    private DataSource createDataSource(final Map<String, DataSource> dataSourceMap) throws SQLException, IOException {
        return "masterslave".equals(shardingRuleType)
                ? YamlMasterSlaveDataSourceFactory.createDataSource(dataSourceMap, new File(EnvironmentPath.getShardingRuleResourceFile(shardingRuleType)))
                : YamlShardingDataSourceFactory.createDataSource(dataSourceMap, new File(EnvironmentPath.getShardingRuleResourceFile(shardingRuleType)));
    }
    
    private Map<String, DataSource> createInstanceDataSourceMap() throws SQLException {
        return "masterslave".equals(shardingRuleType) ? dataSourceMap : getShardingInstanceDataSourceMap();
    }
    
    private Map<String, DataSource> getShardingInstanceDataSourceMap() throws SQLException {
        Map<String, DataSource> result = new LinkedHashMap<>();
        Map<String, DataSourceMetaData> dataSourceMetaDataMap = getDataSourceMetaDataMap();
        for (Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
            if (!isExisted(entry.getKey(), result.keySet(), dataSourceMetaDataMap)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
    
    private boolean isExisted(final String dataSourceName, final Collection<String> existedDataSourceNames, final Map<String, DataSourceMetaData> dataSourceMetaDataMap) {
        for (String each : existedDataSourceNames) {
            if (dataSourceMetaDataMap.get(each).isInSameDatabaseInstance(dataSourceMetaDataMap.get(dataSourceName))) {
                return true;
            }
        }
        return false;
    }
    
    private Map<String, DataSourceMetaData> getDataSourceMetaDataMap() throws SQLException {
        Map<String, DataSourceMetaData> result = new LinkedHashMap<>();
        for (Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
            result.put(entry.getKey(), databaseTypeEnvironment.getDatabaseType().getDataSourceMetaData(getDataSourceURL(entry.getValue())));
        }
        return result;
    }
    
    private static String getDataSourceURL(final DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getURL();
        }
    }

    protected static void createDatabasesAndTables() {
        createDatabases();
        dropTables();
        createTables();
    }

    protected static void createDatabases() {
        try {
            for (String each : integrateTestEnvironment.getShardingRuleTypes()) {
                SchemaEnvironmentManager.dropDatabase(each);
            }
            for (String each : integrateTestEnvironment.getShardingRuleTypes()) {
                SchemaEnvironmentManager.createDatabase(each);
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    protected static void createTables() {
        try {
            for (String each : integrateTestEnvironment.getShardingRuleTypes()) {
                SchemaEnvironmentManager.createTable(each);
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    protected static void dropDatabases() {
        try {
            for (String each : integrateTestEnvironment.getShardingRuleTypes()) {
                SchemaEnvironmentManager.dropDatabase(each);
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    protected static void dropTables() {
        try {
            for (String each : integrateTestEnvironment.getShardingRuleTypes()) {
                SchemaEnvironmentManager.dropTable(each);
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        if (dataSource instanceof ShardingDataSource) {
            ((ShardingDataSource) dataSource).getShardingContext().getExecuteEngine().close();
        }
    }
}

