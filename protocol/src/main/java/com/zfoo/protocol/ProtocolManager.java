/*
 * Copyright (C) 2020 The zfoo Authors
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.zfoo.protocol;

import com.zfoo.protocol.buffer.ByteBufUtils;
import com.zfoo.protocol.generate.GenerateOperation;
import com.zfoo.protocol.registration.IProtocolRegistration;
import com.zfoo.protocol.registration.ProtocolAnalysis;
import com.zfoo.protocol.registration.ProtocolModule;
import com.zfoo.protocol.util.AssertionUtils;
import com.zfoo.protocol.xml.XmlProtocols;
import io.netty.buffer.ByteBuf;

import java.util.*;

/**
 * @author jaysunxiao
 * @version 3.0
 */
public class ProtocolManager {

    public static final String PROTOCOL_ID = "PROTOCOL_ID";
    public static final short MAX_PROTOCOL_NUM = Short.MAX_VALUE;
    public static final byte MAX_MODULE_NUM = Byte.MAX_VALUE;

    /**
     * 索引：协议号protocolId
     */
    public static final IProtocolRegistration[] protocols = new IProtocolRegistration[MAX_PROTOCOL_NUM];
    /**
     * 索引：模块号
     */
    public static final ProtocolModule[] modules = new ProtocolModule[MAX_MODULE_NUM];

    /**
     * key:协议class
     * value:protocolId
     */
    private static final Map<Class<?>, Short> protocolIdMap = new HashMap<>();

    static {
        // 初始化默认协议模块
        modules[0] = ProtocolModule.DEFAULT_PROTOCOL_MODULE;
    }

    /**
     * 将packet序列化到buffer中
     */
    public static void write(ByteBuf buffer, IPacket packet) {
        var protocolId = packet.protocolId();
        // 写入协议号
        ByteBufUtils.writeShort(buffer, protocolId);
        // 写入包体
        protocols[protocolId].write(buffer, packet);
    }

    public static IPacket read(ByteBuf buffer) {
        return (IPacket) protocols[ByteBufUtils.readShort(buffer)].read(buffer);
    }

    public static IProtocolRegistration getProtocol(short protocolId) {
        var protocol = protocols[protocolId];
        AssertionUtils.notNull(protocol, "[protocolId:{}]协议不存在，可能没有注册该协议或者协议号错误", protocolId);
        return protocol;
    }

    public static ProtocolModule moduleByProtocolId(short id) {
        return modules[protocols[id].module()];
    }

    /**
     * 根据模块id查找模块
     *
     * @param moduleId
     * @return
     */
    public static ProtocolModule moduleByModuleId(byte moduleId) {
        var module = modules[moduleId];
        AssertionUtils.notNull(module, "[moduleId:{}]不存在", moduleId);
        return module;
    }

    /**
     * 根据模块名字查找模块
     *
     * @param name
     * @return
     */
    public static ProtocolModule moduleByModuleName(String name) {
        var moduleOptional = Arrays.stream(modules)
                .filter(it -> Objects.nonNull(it))
                .filter(it -> it.getName().equals(name))
                .findFirst();
        if (moduleOptional.isEmpty()) {
            return null;
        }
        return moduleOptional.get();
    }

    public static short protocolId(Class<?> clazz) {
        var protocolId = protocolIdMap.get(clazz);
        if (protocolId == null) {
            protocolId = ProtocolAnalysis.getProtocolIdByClass(clazz);
            // 使用懒加载的方式，主要是想节省一点空间，实际运行只有IPacket没有重写protocolId的Response和Answer才会放进去
            synchronized (protocolIdMap) {
                protocolIdMap.put(clazz, protocolId);
            }
        }
        return protocolId;
    }

    public static void initProtocol(Set<Class<?>> protocolClassSet) {
        ProtocolAnalysis.analyze(protocolClassSet, GenerateOperation.NO_OPERATION);
    }

    /**
     * 注册协议：将协议id和对应的协议信息关联起来
     *
     * @param protocolClassSet  需要初始化的协议列表
     * @param generateOperation 协议配置：需要生成哪些语言的协议文件 是否折叠等信息
     */
    public static void initProtocol(Set<Class<?>> protocolClassSet, GenerateOperation generateOperation) {
        ProtocolAnalysis.analyze(protocolClassSet, generateOperation);
    }

    public static void initProtocol(XmlProtocols xmlProtocols, GenerateOperation generateOperation) {
        ProtocolAnalysis.analyze(xmlProtocols, generateOperation);
    }

}
