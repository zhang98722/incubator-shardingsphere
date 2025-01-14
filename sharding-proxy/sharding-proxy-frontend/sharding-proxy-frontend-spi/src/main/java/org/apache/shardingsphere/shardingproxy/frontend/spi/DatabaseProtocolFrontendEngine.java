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

package org.apache.shardingsphere.shardingproxy.frontend.spi;

import org.apache.shardingsphere.shardingproxy.backend.communication.jdbc.connection.BackendConnection;
import org.apache.shardingsphere.shardingproxy.frontend.context.FrontendContext;
import org.apache.shardingsphere.shardingproxy.frontend.engine.AuthenticationEngine;
import org.apache.shardingsphere.shardingproxy.frontend.engine.CommandExecuteEngine;
import org.apache.shardingsphere.shardingproxy.transport.codec.DatabasePacketCodecEngine;
import org.apache.shardingsphere.spi.database.DatabaseType;

/**
 * Database protocol frontend engine.
 * 
 * @author zhangliang 
 */
public interface DatabaseProtocolFrontendEngine {
    
    /**
     * Get database type.
     * 
     * @return database type
     */
    DatabaseType getDatabaseType();
    
    /**
     * Get frontend context.
     *
     * @return frontend context
     */
    FrontendContext getFrontendContext();
    
    /**
     * Get database packet codec engine.
     * 
     * @return database packet codec engine
     */
    DatabasePacketCodecEngine getCodecEngine();
    
    /**
     * Get authentication engine.
     * 
     * @return authentication engine
     */
    AuthenticationEngine getAuthEngine();
    
    /**
     * Get command execute engine.
     * 
     * @return command execute engine
     */
    CommandExecuteEngine getCommandExecuteEngine();
    
    /**
     * Release resource.
     * 
     * @param backendConnection backend connection
     */
    void release(BackendConnection backendConnection);
}
